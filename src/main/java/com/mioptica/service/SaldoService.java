package com.mioptica.service;

import com.mioptica.model.*;
import com.mioptica.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SaldoService {

    private final FichaClinicaRepository fichaRepo;
    private final ReciboCajaRepository   reciboRepo;
    private final CorrelativoRepository  correlativoRepo;
    private final SucursalRepository     sucursalRepo;
    private final UsuarioRepository      usuarioRepo;
    private final ClienteRepository      clienteRepo;

    // ─── Saldos pendientes ────────────────────────────────────────
    public List<FichaClinica> saldosPendientes(Integer idSucursal, boolean esAdmin) {
        return esAdmin
                ? fichaRepo.findConSaldoPendiente()
                : fichaRepo.findConSaldoPendienteBySucursal(idSucursal);
    }

    // ─── Entregas pendientes ──────────────────────────────────────
    public List<FichaClinica> entregasPendientes(Integer idSucursal, boolean esAdmin) {
        return esAdmin
                ? fichaRepo.findEntregasPendientes()
                : fichaRepo.findEntregasPendientesBySucursal(idSucursal);
    }

    // ─── Registrar pago ───────────────────────────────────────────
    @Transactional
    public BigDecimal registrarPago(Integer idFicha,
                                    BigDecimal monto,
                                    String formaPago,
                                    String nota,
                                    Integer idUsuario) throws Exception {

        FichaClinica ficha = fichaRepo.findById(idFicha)
                .orElseThrow(() -> new Exception("Ficha no encontrada."));

        if (monto == null || monto.compareTo(BigDecimal.ZERO) <= 0)
            throw new Exception("El monto debe ser mayor a 0.");

        if (monto.compareTo(ficha.getSaldo()) > 0)
            throw new Exception("El monto (Q" + monto + ") supera el saldo (Q" + ficha.getSaldo() + ").");

        // Actualizar saldo en ficha
        BigDecimal nuevoSaldo = ficha.getSaldo().subtract(monto);
        ficha.setSaldo(nuevoSaldo);
        ficha.setAbono((ficha.getAbono() != null ? ficha.getAbono() : BigDecimal.ZERO).add(monto));
        fichaRepo.save(ficha);

        // Generar recibo de caja
        Usuario  usuario  = usuarioRepo.findById(idUsuario).orElseThrow();
        Sucursal sucursal = ficha.getSucursal();

        String numRecibo = generarCorrelativo(sucursal.getIdSucursal(), "Recibo", "RC");

        ReciboCaja recibo = new ReciboCaja();
        recibo.setSucursal(sucursal);
        recibo.setUsuario(usuario);
        recibo.setCliente(ficha.getCliente());
        recibo.setNumeroRecibo(numRecibo);
        recibo.setFecha(LocalDate.now());
        recibo.setMonto(monto);
        recibo.setFormaPago(formaPago != null ? formaPago : "Contado");
        recibo.setConcepto("Abono ficha #" + idFicha +
                (nota != null && !nota.isBlank() ? ". " + nota : ""));
        reciboRepo.save(recibo);

        return nuevoSaldo;
    }

    // ─── Marcar entrega ───────────────────────────────────────────
    @Transactional
    public void marcarEntregado(Integer idFicha) throws Exception {
        FichaClinica ficha = fichaRepo.findById(idFicha)
                .orElseThrow(() -> new Exception("Ficha no encontrada."));

        ficha.setEstadoEntrega("Entregado");
        ficha.setFechaEntregaReal(LocalDate.now());
        fichaRepo.save(ficha);
    }

    // ─── Stats para la vista ──────────────────────────────────────
    public BigDecimal totalSaldoPendiente(Integer idSucursal, boolean esAdmin) {
        return saldosPendientes(idSucursal, esAdmin).stream()
                .map(FichaClinica::getSaldo)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public long entregasVencidas(Integer idSucursal, boolean esAdmin) {
        return entregasPendientes(idSucursal, esAdmin).stream()
                .filter(f -> f.getDiasParaEntrega() < 0)
                .count();
    }

    public long entregasProximas7Dias(Integer idSucursal, boolean esAdmin) {
        return entregasPendientes(idSucursal, esAdmin).stream()
                .filter(f -> {
                    long d = f.getDiasParaEntrega();
                    return d >= 0 && d <= 7;
                })
                .count();
    }

    // ─── Helper: correlativo ──────────────────────────────────────
    private String generarCorrelativo(Integer idSucursal, String tipo, String prefijo) {
        Sucursal suc = sucursalRepo.findById(idSucursal).orElseThrow();
        Correlativo corr = correlativoRepo
                .findBySucursalAndTipo(idSucursal, tipo)
                .orElseGet(() -> {
                    Correlativo c = new Correlativo();
                    c.setSucursal(suc);
                    c.setTipo(tipo);
                    c.setValorActual(0);
                    return c;
                });
        corr.setValorActual(corr.getValorActual() + 1);
        correlativoRepo.save(corr);
        return prefijo + "-" + idSucursal + "-" + String.format("%05d", corr.getValorActual());
    }
}
