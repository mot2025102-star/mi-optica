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

    private final VentaRepository        ventaRepo;
    private final ReciboCajaRepository   reciboRepo;
    private final CorrelativoRepository  correlativoRepo;
    private final SucursalRepository     sucursalRepo;
    private final UsuarioRepository      usuarioRepo;

    // ─── Saldos pendientes ────────────────────────────────────────
    public List<Venta> saldosPendientes(Integer idSucursal, boolean esAdmin) {
        return esAdmin
                ? ventaRepo.findConSaldoPendiente()
                : ventaRepo.findConSaldoPendienteBySucursal(idSucursal);
    }

    // ─── Entregas pendientes ──────────────────────────────────────
    public List<Venta> entregasPendientes(Integer idSucursal, boolean esAdmin) {
        return esAdmin
                ? ventaRepo.findEntregasPendientes()
                : ventaRepo.findEntregasPendientesBySucursal(idSucursal);
    }

    // ─── Registrar pago sobre una Venta ──────────────────────────
    @Transactional
    public BigDecimal registrarPago(Integer idVenta,
                                    BigDecimal monto,
                                    String formaPago,
                                    String nota,
                                    Integer idUsuario) throws Exception {

        Venta venta = ventaRepo.findById(idVenta)
                .orElseThrow(() -> new Exception("Venta no encontrada."));

        if (monto == null || monto.compareTo(BigDecimal.ZERO) <= 0)
            throw new Exception("El monto debe ser mayor a 0.");

        if (monto.compareTo(venta.getSaldoPendiente()) > 0)
            throw new Exception("El monto (Q" + monto + ") supera el saldo (Q" + venta.getSaldoPendiente() + ").");

        // Actualizar saldo en la venta
        BigDecimal nuevoSaldo = venta.getSaldoPendiente().subtract(monto);
        venta.setSaldoPendiente(nuevoSaldo);

        // Si quedó en 0, marcar como Pagada
        if (nuevoSaldo.compareTo(BigDecimal.ZERO) == 0) {
            venta.setEstado("Pagada");
            venta.setEstadoPago("COMPLETO");
        }

        ventaRepo.save(venta);

        // Generar recibo de caja
        Usuario  usuario  = usuarioRepo.findById(idUsuario).orElseThrow();
        Sucursal sucursal = venta.getSucursal();

        String numRecibo = generarCorrelativo(sucursal.getIdSucursal(), "Recibo", "RC");

        ReciboCaja recibo = new ReciboCaja();
        recibo.setSucursal(sucursal);
        recibo.setUsuario(usuario);
        recibo.setCliente(venta.getCliente());
        recibo.setNumeroRecibo(numRecibo);
        recibo.setFecha(LocalDate.now());
        recibo.setMonto(monto);
        recibo.setFormaPago(formaPago != null ? formaPago : "Contado");
        recibo.setConcepto("Abono factura #" + venta.getNumeroFactura() +
                (nota != null && !nota.isBlank() ? ". " + nota : ""));
        recibo.setVenta(venta);
        reciboRepo.save(recibo);

        return nuevoSaldo;
    }

    // ─── Marcar entrega ───────────────────────────────────────────
    @Transactional
    public void marcarEntregado(Integer idVenta) throws Exception {
        Venta venta = ventaRepo.findById(idVenta)
                .orElseThrow(() -> new Exception("Venta no encontrada."));

        venta.setEstadoEntrega("Entregado");
        venta.setFechaEntregaReal(LocalDate.now());
        ventaRepo.save(venta);
    }

    // ─── Stats para la vista ──────────────────────────────────────
    public BigDecimal totalSaldoPendiente(Integer idSucursal, boolean esAdmin) {
        return saldosPendientes(idSucursal, esAdmin).stream()
                .map(Venta::getSaldoPendiente)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public long entregasVencidas(Integer idSucursal, boolean esAdmin) {
        return entregasPendientes(idSucursal, esAdmin).stream()
                .filter(v -> v.getDiasParaEntrega() < 0)
                .count();
    }

    public long entregasProximas7Dias(Integer idSucursal, boolean esAdmin) {
        return entregasPendientes(idSucursal, esAdmin).stream()
                .filter(v -> {
                    long d = v.getDiasParaEntrega();
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
