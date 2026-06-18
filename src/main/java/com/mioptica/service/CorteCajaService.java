package com.mioptica.service;
 
import com.mioptica.model.*;
import com.mioptica.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
 
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
 
@Service
@RequiredArgsConstructor
public class CorteCajaService {
 
    private final CorteCajaRepository  corteCajaRepo;
    private final GastoCajaRepository  gastoCajaRepo;
    private final VentaRepository      ventaRepo;
    private final SucursalRepository   sucursalRepo;
    private final UsuarioRepository    usuarioRepo;
    private final ReciboCajaRepository reciboRepo;
 
    // ─── Obtener o crear corte del día ────────────────────────────
    public CorteCaja obtenerOCrearCorte(LocalDate fecha, Integer idSucursal, String username) {
        Optional<CorteCaja> existente = corteCajaRepo.findByIdSucursalAndFecha(idSucursal, fecha);
        if (existente.isPresent()) return existente.get();
 
        Sucursal sucursal = sucursalRepo.findById(idSucursal).orElseThrow();
        Usuario  usuario  = usuarioRepo.findByUsername(username).orElseThrow();
 
        CorteCaja nuevo = new CorteCaja();
        nuevo.setSucursal(sucursal);
        nuevo.setUsuario(usuario);
        nuevo.setFecha(fecha);
        nuevo.setSaldoInicial(BigDecimal.ZERO);
        return corteCajaRepo.save(nuevo);
    }
 
    // ─── Recalcular totales del corte ─────────────────────────────
    @Transactional
    public CorteCaja recalcularCorte(CorteCaja corte) {
        LocalDate fecha      = corte.getFecha();
        Integer   idSucursal = corte.getSucursal().getIdSucursal();
 
        // Total recibos del día
        List<ReciboCaja> recibos = reciboRepo.findByFechaYSucursal(fecha, idSucursal);
        BigDecimal totalRecibos = recibos.stream()
                .map(ReciboCaja::getMonto)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
 
        // Desglose por forma de pago desde recibos_caja
        BigDecimal totalEfectivo      = BigDecimal.ZERO;
        BigDecimal totalTarjeta       = BigDecimal.ZERO;
        BigDecimal totalTransferencia = BigDecimal.ZERO;
        BigDecimal totalCheque        = BigDecimal.ZERO;
 
        for (Object[] row : reciboRepo.totalPorFormaPago(fecha, LocalDate.now(), idSucursal)) {
            String forma = row[0] != null ? row[0].toString() : "Contado";
            BigDecimal monto = toBD(row[1]);
            switch (forma.toLowerCase()) {
                case "tarjeta"       -> totalTarjeta       = totalTarjeta.add(monto);
                case "transferencia" -> totalTransferencia = totalTransferencia.add(monto);
                case "cheque"        -> totalCheque        = totalCheque.add(monto);
                default              -> totalEfectivo      = totalEfectivo.add(monto);
            }
        }
 
        // Total gastos del día
        BigDecimal totalGastos = gastoCajaRepo.totalGastosDia(fecha, idSucursal);
        if (totalGastos == null) totalGastos = BigDecimal.ZERO;
 
        // Saldo esperado = saldo inicial + recibos - gastos
        BigDecimal saldoEsperado = corte.getSaldoInicial()
                .add(totalRecibos).subtract(totalGastos);
 
        // Diferencia = saldo físico - saldo esperado
        BigDecimal diferencia = BigDecimal.ZERO;
        if (corte.getSaldoFisico() != null)
            diferencia = corte.getSaldoFisico().subtract(saldoEsperado);
 
        corte.setTotalVentas(totalRecibos);
        corte.setTotalGastos(totalGastos);
        corte.setTotalEfectivo(totalEfectivo);
        corte.setTotalTarjeta(totalTarjeta);
        corte.setTotalTransferencia(totalTransferencia);
        corte.setTotalCheque(totalCheque);
        corte.setSaldoEsperado(saldoEsperado);
        corte.setDiferencia(diferencia);
 
        return corteCajaRepo.save(corte);
    }
 
    // ─── Actualizar saldo inicial ─────────────────────────────────
    @Transactional
    public CorteCaja actualizarSaldoInicial(Integer idCorte, BigDecimal saldoInicial) throws Exception {
        CorteCaja corte = corteCajaRepo.findById(idCorte)
                .orElseThrow(() -> new Exception("Corte no encontrado."));
        if (corte.getCerrado()) throw new Exception("El corte ya está cerrado.");
        corte.setSaldoInicial(saldoInicial);
        return recalcularCorte(corte);
    }
 
    // ─── Registrar gasto ─────────────────────────────────────────
    @Transactional
    public GastoCaja registrarGasto(LocalDate fecha, Integer idSucursal,
                                     String username, String concepto,
                                     BigDecimal monto) throws Exception {
        Sucursal sucursal = sucursalRepo.findById(idSucursal).orElseThrow();
        Usuario  usuario  = usuarioRepo.findByUsername(username).orElseThrow();
 
        GastoCaja gasto = new GastoCaja();
        gasto.setSucursal(sucursal);
        gasto.setUsuario(usuario);
        gasto.setFecha(fecha);
        gasto.setConcepto(concepto);
        gasto.setMonto(monto);
        gastoCajaRepo.save(gasto);
 
        recalcularCorte(obtenerOCrearCorte(fecha, idSucursal, username));
        return gasto;
    }
 
    // ─── Eliminar gasto ───────────────────────────────────────────
    @Transactional
    public void eliminarGasto(Integer idGasto, String username) throws Exception {
        GastoCaja gasto = gastoCajaRepo.findById(idGasto)
                .orElseThrow(() -> new Exception("Gasto no encontrado."));
        gastoCajaRepo.delete(gasto);
        recalcularCorte(obtenerOCrearCorte(
                gasto.getFecha(), gasto.getSucursal().getIdSucursal(), username));
    }
 
    // ─── Cerrar corte ─────────────────────────────────────────────
    @Transactional
    public CorteCaja cerrarCorte(Integer idCorte, BigDecimal saldoFisico,
                                  String observacion) throws Exception {
        CorteCaja corte = corteCajaRepo.findById(idCorte)
                .orElseThrow(() -> new Exception("Corte no encontrado."));
        if (corte.getCerrado()) throw new Exception("El corte ya está cerrado.");
 
        corte.setSaldoFisico(saldoFisico);
        corte.setObservacion(observacion);
        corte.setCerrado(true);
        corte.setFechaCierre(LocalDate.now());
        CorteCaja cerrado = recalcularCorte(corte);
 
    
        
        return cerrado;
    }
 
    // ─── Obtener gastos del día ───────────────────────────────────
    public List<GastoCaja> obtenerGastos(LocalDate fecha, Integer idSucursal) {
        return gastoCajaRepo.findByFechaYSucursal(fecha, idSucursal);
    }
 
    // ─── Historial ───────────────────────────────────────────────
    public List<CorteCaja> historial(Integer idSucursal) {
        return corteCajaRepo.findBySucursal(idSucursal);
    }
    // ─── Nuevo corte ──────────────────────────────────────────────
        @Transactional
        public CorteCaja nuevoCorte(Integer idSucursal, String username) throws Exception {
        // Verificar que no haya corte abierto
        Optional<CorteCaja> abierto = corteCajaRepo.findAbiertoBySucursal(idSucursal);
        if (abierto.isPresent()) throw new Exception("Ya hay un corte abierto.");

        Sucursal sucursal = sucursalRepo.findById(idSucursal).orElseThrow();
        Usuario  usuario  = usuarioRepo.findByUsername(username).orElseThrow();

        CorteCaja nuevo = new CorteCaja();
        nuevo.setSucursal(sucursal);
        nuevo.setUsuario(usuario);
        LocalDate fechaNueva = LocalDate.now();
        while (corteCajaRepo.existsBySucursalIdSucursalAndFecha(idSucursal, fechaNueva)) {
        fechaNueva = fechaNueva.plusDays(1);
        }
        nuevo.setFecha(fechaNueva);
        nuevo.setSaldoInicial(BigDecimal.ZERO);
        nuevo.setCerrado(false);
        return corteCajaRepo.save(nuevo);
        }
    private BigDecimal toBD(Object val) {
        if (val instanceof BigDecimal bd) return bd;
        if (val != null) return new BigDecimal(val.toString());
        return BigDecimal.ZERO;
    }
}