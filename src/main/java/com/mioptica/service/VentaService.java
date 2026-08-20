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

import java.math.RoundingMode;

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
    private final OrdenLaboratorioRepository ordenRepo;  // para venta con orden externa

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

        BigDecimal pctGlobal = req.getDescuentoGlobal() != null
                ? req.getDescuentoGlobal() : BigDecimal.ZERO;

        BigDecimal descGlobal = subtotal
                .multiply(pctGlobal)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

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
        // ── Procesar Pagos y Saldo ───────────────────────────
        BigDecimal totalPagado = BigDecimal.ZERO;
        if (req.getPagos() != null && !req.getPagos().isEmpty()) {
            // BUG FIX 1: Si el usuario dejó el monto en 0 pero hay un solo método de pago, asumimos que paga el total completo.
            BigDecimal sumaPagosEnviada = req.getPagos().stream()
                    .map(p -> p.getMonto() != null ? p.getMonto() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            if (sumaPagosEnviada.compareTo(BigDecimal.ZERO) <= 0 && req.getPagos().size() == 1) {
                req.getPagos().get(0).setMonto(total);
            }

            for (VentaRequest.PagoRequest pReq : req.getPagos()) {
                if (pReq.getMonto() != null && pReq.getMonto().compareTo(BigDecimal.ZERO) > 0) {
                    totalPagado = totalPagado.add(pReq.getMonto());
                    PagoVenta pago = new PagoVenta();
                    pago.setVenta(venta);
                    pago.setMetodoPago(pReq.getMetodoPago() != null ? pReq.getMetodoPago() : "Contado");
                    pago.setMonto(pReq.getMonto());
                    pago.setTipoTarjeta(pReq.getTipoTarjeta());
                    pago.setMarcaTarjeta(pReq.getMarcaTarjeta());
                    pago.setUltimosDigitos(pReq.getUltimosDigitos());
                    pago.setAutorizacion(pReq.getAutorizacion());
                    pago.setBanco(pReq.getBanco());
                    pago.setNoCheque(pReq.getNoCheque());
                    pago.setTitular(pReq.getTitular());
                    pago.setFechaCheque(pReq.getFechaCheque());
                    if ("Transferencia".equalsIgnoreCase(pago.getMetodoPago()) && pReq.getReferencia() != null) {
                        pago.setAutorizacion(pReq.getReferencia());
                    }
                    venta.getPagos().add(pago);
                }
            }
        }
        
        BigDecimal saldoPendiente = total.subtract(totalPagado);
        if (saldoPendiente.compareTo(BigDecimal.ZERO) <= 0) {
            // Pagó todo (o más del total con vuelto)
            venta.setSaldoPendiente(BigDecimal.ZERO);
            venta.setEstadoPago("COMPLETO");
            venta.setEstado("Pagada");
        } else {
            // Hay saldo pendiente → distinguir ANTICIPO vs PENDIENTE según fecha de entrega
            venta.setSaldoPendiente(saldoPendiente);
            // Parsear la fecha de entrega del request (aún no está asignada a la entidad)
            LocalDate fechaEnt = null;
            if (req.getFechaEntrega() != null && !req.getFechaEntrega().isBlank()) {
                try { fechaEnt = LocalDate.parse(req.getFechaEntrega()); } catch (Exception ignored) {}
            }
            // ANTICIPO: el cliente paga ANTES de recibir (fecha entrega es futura)
            // PENDIENTE: ya se entregó o no hay fecha, el cliente aún debe dinero
            if (fechaEnt != null && fechaEnt.isAfter(LocalDate.now())) {
                venta.setEstadoPago("ANTICIPO");
                venta.setEstado("Anticipo");
            } else {
                venta.setEstadoPago("PENDIENTE");
                venta.setEstado("Pendiente");
            }
        }
        
        venta.setObservacion(req.getObservacion());

        // TAREA 2: Fecha de entrega
        if (req.getFechaEntrega() != null && !req.getFechaEntrega().isBlank()) {
            venta.setFechaEntrega(LocalDate.parse(req.getFechaEntrega()));
        }

        // TAREA 3: Lugar y sucursal de entrega
        if (req.getLugarEntrega() != null && !req.getLugarEntrega().isBlank()) {
            venta.setLugarEntrega(req.getLugarEntrega());
        }
        if (req.getIdSucursalEntrega() != null) {
            sucursalRepo.findById(req.getIdSucursalEntrega())
                    .ifPresent(venta::setSucursalEntrega);
        }

        // Cliente (opcional)

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
            BigDecimal pctFila = item.getDescuento() != null ? item.getDescuento() : BigDecimal.ZERO;
            BigDecimal precioUnit = item.getPrecioUnitario() != null ? item.getPrecioUnitario() : BigDecimal.ZERO;
            BigDecimal cantFila   = item.getCantidad() != null      ? item.getCantidad()       : BigDecimal.ZERO;
            BigDecimal montoDescFila = precioUnit.multiply(cantFila)
                    .multiply(pctFila)
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            det.setDescuento(montoDescFila);
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
        if (req.getPagos() != null && !req.getPagos().isEmpty()) {
            for (VentaRequest.PagoRequest p : req.getPagos()) {
                if (p.getMonto() != null && p.getMonto().compareTo(BigDecimal.ZERO) > 0) {
                    String numRecibo = generarCorrelativo(idSucursal, "Recibo", "RC");
                    ReciboCaja recibo = new ReciboCaja();
                    recibo.setSucursal(sucursal);
                    recibo.setUsuario(usuario);
                    recibo.setCliente(venta.getCliente());
                    recibo.setNumeroRecibo(numRecibo);
                    recibo.setFecha(LocalDate.now());
                    
                    // Si el pago excede el total (vuelto), el recibo se hace por el monto pagado para cuadrar caja
                    recibo.setMonto(p.getMonto());
                    
                    recibo.setFormaPago(p.getMetodoPago() != null ? p.getMetodoPago() : "Contado");
                    
                    if ("Transferencia".equalsIgnoreCase(p.getMetodoPago())) {
                        recibo.setReferencia(p.getReferencia() != null ? p.getReferencia() : p.getAutorizacion());
                        recibo.setBanco(p.getBanco());
                    } else if ("Cheque".equalsIgnoreCase(p.getMetodoPago())) {
                        recibo.setReferencia(p.getNoCheque());
                        recibo.setBanco(p.getBanco());
                    } else if ("Tarjeta".equalsIgnoreCase(p.getMetodoPago())) {
                        recibo.setReferencia(p.getAutorizacion());
                    }
                    
                    recibo.setConcepto("Pago factura " + numFactura);
                    recibo.setVenta(venta);
                    reciboRepo.save(recibo);
                }
            }
        }

        // ── 7. Generar Orden de Laboratorio si la venta incluye Lentes ───
        generarOrdenLaboratorio(req, venta, sucursal, usuario);

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
    
    // ── Helper: orden desde venta ────────────────────────
    private void generarOrdenLaboratorio(VentaRequest req, Venta venta,
                                        Sucursal sucursal, Usuario usuario) {
        try {
            // Verificar si hay al menos un producto tipo "LENTE" en el detalle
            boolean tieneLente = false;
            for (VentaRequest.ItemVenta item : req.getItems()) {
                if (item.getIdProducto() != null) {
                    Producto p = productoRepo.findById(item.getIdProducto()).orElse(null);
                    if (p != null && p.esLente()) {
                        tieneLente = true;
                        break;
                    }
                }
            }

            // Si no hay lente, no se crea orden
            if (!tieneLente) {
                return;
            }

            int next = ordenRepo.findMaxId().orElse(0) + 1;

            OrdenLaboratorio orden = new OrdenLaboratorio();
            orden.setNumeroOrden(String.format("OL-%06d", next));
            orden.setSucursal(sucursal);
            orden.setUsuario(usuario);
            orden.setCliente(venta.getCliente());
            orden.setFechaEmision(LocalDate.now());
            orden.setEstado("Pendiente");
            
            // Relación con VENTA
            orden.setOrigen("VENTA");
            orden.setVenta(venta);
            
            if (req.isRecetaExterna()) {
                orden.setNotaOrigen("⚠️ Graduación dada por el cliente");
            } else if (req.isFichaInterna()) {
                orden.setNotaOrigen("✅ Graduación cargada de Ficha Interna");
            } else {
                orden.setNotaOrigen("Orden generada a partir de Venta");
            }

            // Dependiendo de si activó receta externa o ficha interna, extraer datos:
            boolean externa = req.isRecetaExterna();
            
            // Graduación
            orden.setOdEsfera(externa ? req.getRxOdEsfera() : req.getFiOdEsfera());
            orden.setOdCilindro(externa ? req.getRxOdCilindro() : req.getFiOdCilindro());
            orden.setOdEje(externa ? req.getRxOdEje() : req.getFiOdEje());
            orden.setOdAdd(externa ? req.getRxOdAdd() : req.getFiOdAdd());

            orden.setOiEsfera(externa ? req.getRxOiEsfera() : req.getFiOiEsfera());
            orden.setOiCilindro(externa ? req.getRxOiCilindro() : req.getFiOiCilindro());
            orden.setOiEje(externa ? req.getRxOiEje() : req.getFiOiEje());
            orden.setOiAdd(externa ? req.getRxOiAdd() : req.getFiOiAdd());

            // Medidas del paciente (DIP, Alturas, etc.)
            String dipTotal = externa ? req.getRxDip() : req.getFiDip();
            orden.setOdDip(dipTotal);
            orden.setOiDip(dipTotal);
            
            orden.setOdNdpod(externa ? req.getRxDpOd() : req.getFiDpOd());
            orden.setOiNdpoi(externa ? req.getRxDpOi() : req.getFiDpOi());
            
            orden.setOdAltura(externa ? req.getRxAlturaOd() : req.getFiAlturaOd());
            orden.setOiAltura(externa ? req.getRxAlturaOi() : req.getFiAlturaOi());
            
            // Medidas del armazón
            orden.setPantoscopico(externa ? req.getRxPantoscopico() : req.getFiPantoscopico());
            orden.setVertex(externa ? req.getRxVertex() : req.getFiVertex());
            orden.setPanoramico(externa ? req.getRxPanoramico() : req.getFiPanoramico());
            
            StringBuilder obs = new StringBuilder();
            String lenteRec = externa ? req.getRxLenteRecomendado() : req.getFiLenteRecomendado();
            if (lenteRec != null && !lenteRec.isBlank()) {
                obs.append("Lente recomendado: ").append(lenteRec).append("\n");
            }
            
            String segmento = externa ? req.getRxSegmento() : req.getFiSegmento();
            if (segmento != null && !segmento.isBlank()) {
                obs.append("Segmento: ").append(segmento);
            }
            
            if (!obs.isEmpty()) {
                orden.setObservaciones(obs.toString());
            }

            // Fecha estimada de entrega (la UI envía la fecha de la orden en rxFecha... o fiFecha...)
            String fEntr = externa ? req.getFechaEntregaOrden() : req.getFiFechaEntregaOrden();
            if (fEntr != null && !fEntr.isBlank()) {
                orden.setFechaEntregaEstimada(LocalDate.parse(fEntr));
            }

            // --- Agregar productos de tipo lente y armazón a la orden ---
            java.util.List<DetalleOrdenLab> productosLab = new java.util.ArrayList<>();
            for (VentaRequest.ItemVenta item : req.getItems()) {
                if (item.getIdProducto() != null && item.getCantidad() != null && item.getCantidad().compareTo(BigDecimal.ZERO) > 0) {
                    Producto p = productoRepo.findById(item.getIdProducto()).orElse(null);
                    if (p != null && (p.esLente() || p.esArmazon())) {
                        DetalleOrdenLab detLab = new DetalleOrdenLab();
                        detLab.setOrden(orden);
                        detLab.setCodigo(p.getCodigo());
                        detLab.setCantidad(item.getCantidad().intValue());
                        detLab.setDescripcion(p.getDetalle());
                        
                        if (p.getMaterialLente() != null) detLab.setMaterial(p.getMaterialLente().getNombre());
                        if (p.getTratamientoLente() != null) detLab.setTratamiento(p.getTratamientoLente().getNombre());
                        detLab.setColorTinte(p.getColor());
                        
                        productosLab.add(detLab);
                    }
                }
            }
            orden.setProductos(productosLab);

            ordenRepo.save(orden);

        } catch (Exception e) {
            System.err.println("⚠️ No se pudo generar orden de laboratorio desde venta: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
