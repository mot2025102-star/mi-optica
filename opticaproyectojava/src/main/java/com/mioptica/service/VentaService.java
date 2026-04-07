package com.mioptica.service;

import com.mioptica.dto.VentaRequest;
import com.mioptica.model.*;
import com.mioptica.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class VentaService {

    private final VentaRepository          ventaRepo;
    private final InventarioRepository     inventarioRepo;
    private final CorrelativoRepository    correlativoRepo;
    private final ReciboCajaRepository     reciboRepo;
    private final ClienteRepository        clienteRepo;
    private final ProductoRepository       productoRepo;
    private final SucursalRepository       sucursalRepo;
    private final UsuarioRepository        usuarioRepo;
    private final KardexRepository         kardexRepo;

    // ─── Listar ventas ────────────────────────────────────────────
    public List<Venta> listarPorPeriodo(LocalDate fi, LocalDate ff,
                                        Integer idSucursal, boolean esAdmin) {
        if (esAdmin) return ventaRepo.findByPeriodo(fi, ff);
        return ventaRepo.findByPeriodoYSucursal(fi, ff, idSucursal);
    }

    public Optional<Venta> findById(Integer id) {
        return ventaRepo.findById(id);
    }

    // ─── Registrar venta ──────────────────────────────────────────
    @Transactional
    public Venta registrar(VentaRequest req,
                           Integer idSucursal,
                           Integer idUsuario) throws Exception {

        if (req.getItems() == null || req.getItems().isEmpty()) {
            throw new Exception("Debes agregar al menos un producto.");
        }

        Sucursal sucursal = sucursalRepo.findById(idSucursal)
                .orElseThrow(() -> new Exception("Sucursal no encontrada."));
        Usuario usuario = usuarioRepo.findById(idUsuario)
                .orElseThrow(() -> new Exception("Usuario no encontrado."));

        // ── 1. Validar stock antes de hacer cualquier cambio ──────
        for (VentaRequest.ItemVenta item : req.getItems()) {
            if (item.getIdProducto() == null || item.getCantidad() == null
                    || item.getCantidad().compareTo(BigDecimal.ZERO) <= 0) continue;

            Producto prod = productoRepo.findById(item.getIdProducto())
                    .orElseThrow(() -> new Exception("Producto no encontrado: ID " + item.getIdProducto()));

            Inventario inv = inventarioRepo
                    .findByProductoAndSucursal(prod, sucursal)
                    .orElseThrow(() -> new Exception("El producto \"" + prod.getDetalle() + "\" no tiene registro en esta sucursal."));

            if (inv.getExistencia().compareTo(item.getCantidad()) < 0) {
                throw new Exception("Stock insuficiente para \"" + prod.getDetalle()
                        + "\". Disponible: " + inv.getExistencia()
                        + ", solicitado: " + item.getCantidad() + ".");
            }
        }

        // ── 2. Generar número de factura ──────────────────────────
        String numFactura = generarCorrelativo(idSucursal, "Factura", "F");

        // ── 3. Calcular totales ───────────────────────────────────
        BigDecimal subtotal = req.getItems().stream()
                .filter(i -> i.getSubtotal() != null)
                .map(VentaRequest.ItemVenta::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal descGlobal = req.getDescuentoGlobal() != null
                ? req.getDescuentoGlobal() : BigDecimal.ZERO;

        BigDecimal total = subtotal.subtract(descGlobal).max(BigDecimal.ZERO);

        // ── 4. Crear cabecera de venta ────────────────────────────
        Venta venta = new Venta();
        venta.setSucursal(sucursal);
        venta.setUsuario(usuario);
        venta.setFecha(LocalDate.now());
        venta.setNumeroFactura(numFactura);
        venta.setSubtotal(subtotal);
        venta.setDescuento(descGlobal);
        venta.setTotal(total);
        venta.setEstado("Pagada");
        venta.setObservacion(req.getObservacion());

        // Cliente (opcional)
        if (req.getIdCliente() != null) {
            clienteRepo.findById(req.getIdCliente())
                    .ifPresent(venta::setCliente);
        }

        // ── 5. Crear detalle + descontar inventario + kardex ──────
        for (VentaRequest.ItemVenta item : req.getItems()) {
            if (item.getIdProducto() == null
                    || item.getCantidad() == null
                    || item.getCantidad().compareTo(BigDecimal.ZERO) <= 0) continue;

            Producto prod = productoRepo.findById(item.getIdProducto()).get();
            Inventario inv = inventarioRepo.findByProductoAndSucursal(prod, sucursal).get();

            // Detalle de la venta
            DetalleVenta det = new DetalleVenta();
            det.setVenta(venta);
            det.setProducto(prod);
            det.setCantidad(item.getCantidad());
            det.setPrecioUnitario(item.getPrecioUnitario() != null ? item.getPrecioUnitario() : BigDecimal.ZERO);
            det.setDescuento(item.getDescuento() != null ? item.getDescuento() : BigDecimal.ZERO);
            det.setSubtotal(item.getSubtotal() != null ? item.getSubtotal() : BigDecimal.ZERO);
            venta.getDetalles().add(det);

            // Descontar inventario
            BigDecimal existenciaAnterior = inv.getExistencia();
            BigDecimal existenciaNueva = existenciaAnterior.subtract(item.getCantidad());
            inv.setExistencia(existenciaNueva);
            inventarioRepo.save(inv);

            // Registrar en Kardex
            Kardex k = new Kardex();
            k.setProducto(prod);
            k.setSucursal(sucursal);
            k.setUsuario(usuario);
            k.setTipoMovimiento("Salida");
            k.setReferencia(numFactura);
            k.setFecha(LocalDate.now());
            k.setCantidad(BigDecimal.ZERO);
            k.setPrecioUnitario(BigDecimal.ZERO);
            k.setEgreso(item.getCantidad());
            k.setFechaEgreso(LocalDate.now());
            k.setPrecioVenta(item.getPrecioUnitario() != null ? item.getPrecioUnitario() : BigDecimal.ZERO);
            k.setExistenciaAnterior(existenciaAnterior);
            k.setExistenciaNueva(existenciaNueva);
            k.setObservacion("Venta " + numFactura);
            kardexRepo.save(k);
        }

        venta = ventaRepo.save(venta);

        // ── 6. Recibo de caja automático ──────────────────────────
        String numRecibo = generarCorrelativo(idSucursal, "Recibo", "RC");
        ReciboCaja recibo = new ReciboCaja();
        recibo.setSucursal(sucursal);
        recibo.setUsuario(usuario);
        recibo.setCliente(venta.getCliente());
        recibo.setNumeroRecibo(numRecibo);
        recibo.setFecha(LocalDate.now());
        recibo.setMonto(total);
        recibo.setFormaPago(req.getFormaPago() != null ? req.getFormaPago() : "Contado");
        recibo.setConcepto("Pago factura " + numFactura);
        recibo.setVenta(venta);
        reciboRepo.save(recibo);

        return venta;
    }

    // ─── Anular venta ─────────────────────────────────────────────
    @Transactional
    public void anular(Integer idVenta, Integer idSucursal) throws Exception {
        Venta venta = ventaRepo.findById(idVenta)
                .orElseThrow(() -> new Exception("Venta no encontrada."));

        if ("Anulada".equals(venta.getEstado())) {
            throw new Exception("Esta venta ya fue anulada.");
        }

        Sucursal sucursal = venta.getSucursal();

        // Restaurar inventario por cada ítem
        for (DetalleVenta det : venta.getDetalles()) {
            inventarioRepo.findByProductoAndSucursal(det.getProducto(), sucursal)
                    .ifPresent(inv -> {
                        BigDecimal existenciaAnterior = inv.getExistencia();
                        BigDecimal existenciaNueva = existenciaAnterior.add(det.getCantidad());
                        inv.setExistencia(existenciaNueva);
                        inventarioRepo.save(inv);

                        Kardex k = new Kardex();
                        k.setProducto(det.getProducto());
                        k.setSucursal(sucursal);
                        k.setTipoMovimiento("Entrada");
                        k.setReferencia("ANULACION-" + venta.getNumeroFactura());
                        k.setFecha(LocalDate.now());
                        k.setCantidad(det.getCantidad());
                        k.setPrecioUnitario(det.getPrecioUnitario());
                        k.setEgreso(BigDecimal.ZERO);
                        k.setExistenciaAnterior(existenciaAnterior);
                        k.setExistenciaNueva(existenciaNueva);
                        k.setObservacion("Anulación de " + venta.getNumeroFactura());
                        kardexRepo.save(k);
                    });
        }

        venta.setEstado("Anulada");
        ventaRepo.save(venta);
    }

    // ─── Stats para dashboard ─────────────────────────────────────
    public BigDecimal totalHoy(Integer idSucursal) {
        return ventaRepo.totalHoy(LocalDate.now(), idSucursal);
    }

    public long countHoy(Integer idSucursal) {
        return ventaRepo.countHoy(LocalDate.now(), idSucursal);
    }

    // ─── Helper: generar correlativo ─────────────────────────────
    private String generarCorrelativo(Integer idSucursal, String tipo, String prefijo) {
        Sucursal suc = sucursalRepo.findById(idSucursal).orElseThrow();
        Correlativo corr = correlativoRepo
                .findBySucursalAndTipo(idSucursal, tipo)
                .orElseGet(() -> {
                    Correlativo nuevo = new Correlativo();
                    nuevo.setSucursal(suc);
                    nuevo.setTipo(tipo);
                    nuevo.setValorActual(0);
                    return nuevo;
                });

        corr.setValorActual(corr.getValorActual() + 1);
        correlativoRepo.save(corr);

        return prefijo + "-" + idSucursal + "-"
                + String.format("%06d", corr.getValorActual());
    }
}
