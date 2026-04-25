package com.mioptica.service;

import com.mioptica.model.*;
import com.mioptica.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    // ─── Obtener o crear corte del día ────────────────────────────
    // Si ya existe un corte para esa fecha y sucursal lo devuelve
    // Si no existe crea uno nuevo con saldo inicial 0
    public CorteCaja obtenerOCrearCorte(LocalDate fecha, Integer idSucursal,
                                         String username) {
        Optional<CorteCaja> existente = corteCajaRepo
                .findByIdSucursalAndFecha(idSucursal, fecha);

        if (existente.isPresent()) return existente.get();

        // Crear nuevo corte
        Sucursal sucursal = sucursalRepo.findById(idSucursal).orElseThrow();
        Usuario  usuario  = usuarioRepo.findByUsername(username).orElseThrow();

        CorteCaja nuevo = new CorteCaja();
        nuevo.setSucursal(sucursal);
        nuevo.setUsuario(usuario);
        nuevo.setFecha(fecha);
        nuevo.setSaldoInicial(BigDecimal.ZERO);
        return corteCajaRepo.save(nuevo);
    }

    // ─── Calcular y actualizar totales del corte ──────────────────
    // Recalcula ventas, gastos, saldo esperado y diferencia
    @Transactional
    public CorteCaja recalcularCorte(CorteCaja corte) {

        LocalDate fecha     = corte.getFecha();
        Integer   idSucursal = corte.getSucursal().getIdSucursal();

        // Total ventas del día
        BigDecimal totalVentas = ventaRepo
                .findByPeriodoYSucursal(fecha, fecha, idSucursal)
                .stream()
                .filter(v -> !"Anulada".equals(v.getEstado()))
                .map(Venta::getTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Total gastos del día
        BigDecimal totalGastos = gastoCajaRepo
                .totalGastosDia(fecha, idSucursal);

        // Saldo esperado = saldo inicial + ventas - gastos
        BigDecimal saldoEsperado = corte.getSaldoInicial()
                .add(totalVentas)
                .subtract(totalGastos);

        // Diferencia = saldo físico - saldo esperado
        BigDecimal diferencia = BigDecimal.ZERO;
        if (corte.getSaldoFisico() != null) {
            diferencia = corte.getSaldoFisico().subtract(saldoEsperado);
        }

        corte.setTotalVentas(totalVentas);
        corte.setTotalGastos(totalGastos);
        corte.setSaldoEsperado(saldoEsperado);
        corte.setDiferencia(diferencia);

        return corteCajaRepo.save(corte);
    }

    // ─── Actualizar saldo inicial ─────────────────────────────────
    @Transactional
    public CorteCaja actualizarSaldoInicial(Integer idCorte,
                                             BigDecimal saldoInicial) throws Exception {
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

        // Recalcular el corte del día
        CorteCaja corte = obtenerOCrearCorte(fecha, idSucursal, username);
        recalcularCorte(corte);

        return gasto;
    }

    // ─── Eliminar gasto ───────────────────────────────────────────
    @Transactional
    public void eliminarGasto(Integer idGasto, String username) throws Exception {
        GastoCaja gasto = gastoCajaRepo.findById(idGasto)
                .orElseThrow(() -> new Exception("Gasto no encontrado."));

        gastoCajaRepo.delete(gasto);

        // Recalcular el corte del día
        CorteCaja corte = obtenerOCrearCorte(
                gasto.getFecha(),
                gasto.getSucursal().getIdSucursal(),
                username);
        recalcularCorte(corte);
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
        return recalcularCorte(corte);
    }

    // ─── Obtener gastos del día ───────────────────────────────────
    public java.util.List<GastoCaja> obtenerGastos(LocalDate fecha,
                                                    Integer idSucursal) {
        return gastoCajaRepo.findByFechaYSucursal(fecha, idSucursal);
    }

    // ─── Historial de cortes ──────────────────────────────────────
    public java.util.List<CorteCaja> historial(Integer idSucursal) {
        return corteCajaRepo.findBySucursal(idSucursal);
    }
}
