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
public class InventarioService {

    private final InventarioRepository inventarioRepo;
    private final KardexRepository     kardexRepo;
    private final ProductoRepository   productoRepo;
    private final SucursalRepository   sucursalRepo;
    private final UsuarioRepository    usuarioRepo;

    // ─── Listar inventario ────────────────────────────────────────
    public List<Inventario> listarTodo() {
        return inventarioRepo.findTodosConProductoActivo();
    }

    public List<Inventario> listarPorSucursal(Integer idSucursal) {
        Sucursal suc = sucursalRepo.findById(idSucursal).orElseThrow();
        return inventarioRepo.findBySucursal(suc);
    }

    // ─── Ajuste manual de stock ───────────────────────────────────
    @Transactional
    public void ajustarStock(Integer idProducto,
                             Integer idSucursal,
                             BigDecimal nuevaExistencia,
                             BigDecimal nuevoCosto,
                             BigDecimal nuevoPrecio,
                             String motivo,
                             Integer idUsuario) throws Exception {

        Producto producto = productoRepo.findById(idProducto)
                .orElseThrow(() -> new Exception("Producto no encontrado."));
        Sucursal sucursal = sucursalRepo.findById(idSucursal)
                .orElseThrow(() -> new Exception("Sucursal no encontrada."));
        Usuario  usuario  = usuarioRepo.findById(idUsuario)
                .orElseThrow(() -> new Exception("Usuario no encontrado."));

        Inventario inv = inventarioRepo
                .findByProductoAndSucursal(producto, sucursal)
                .orElseGet(() -> {
                    Inventario nuevo = new Inventario();
                    nuevo.setProducto(producto);
                    nuevo.setSucursal(sucursal);
                    nuevo.setExistencia(BigDecimal.ZERO);
                    return nuevo;
                });

        BigDecimal existenciaAnterior = inv.getExistencia();

        inv.setExistencia(nuevaExistencia);
        if (nuevoCosto  != null) inv.setCosto(nuevoCosto);
        if (nuevoPrecio != null) inv.setPrecioVenta(nuevoPrecio);
        inventarioRepo.save(inv);

        Kardex k = new Kardex();
        k.setProducto(producto);
        k.setSucursal(sucursal);
        k.setTipoMovimiento("Ajuste");
        k.setReferencia("AJUSTE-MANUAL");
        k.setFecha(LocalDate.now());
        k.setCantidad(nuevaExistencia.subtract(existenciaAnterior).abs());
        k.setPrecioUnitario(nuevoCosto != null ? nuevoCosto : inv.getCosto());
        k.setPrecioVenta(nuevoPrecio != null ? nuevoPrecio : inv.getPrecioVenta());
        k.setEgreso(BigDecimal.ZERO);
        k.setExistenciaAnterior(existenciaAnterior);
        k.setExistenciaNueva(nuevaExistencia);
        k.setObservacion("Ajuste manual. " + (motivo != null ? motivo : ""));
        k.setUsuario(usuario);
        kardexRepo.save(k);
    }

    // ─── Eliminar registro de inventario ─────────────────────────
    @Transactional
    public void eliminarInventario(Integer idInventario) throws Exception {
        Inventario inv = inventarioRepo.findById(idInventario)
                .orElseThrow(() -> new Exception("Registro de inventario no encontrado."));
        inventarioRepo.delete(inv);
    }

    // ─── Agregar ingreso ──────────────────────────────────────────
    @Transactional
    public void agregarIngreso(Integer idProducto,
                               Integer idSucursal,
                               BigDecimal cantidad,
                               BigDecimal precioCompra,
                               BigDecimal precioVenta,
                               LocalDate fechaIngreso,
                               String observacion,
                               Integer idUsuario) throws Exception {

        Producto producto = productoRepo.findById(idProducto)
                .orElseThrow(() -> new Exception("Producto no encontrado."));
        Sucursal sucursal = sucursalRepo.findById(idSucursal)
                .orElseThrow(() -> new Exception("Sucursal no encontrada."));
        Usuario  usuario  = usuarioRepo.findById(idUsuario)
                .orElseThrow(() -> new Exception("Usuario no encontrado."));

        List<Kardex> historial = kardexRepo.findByProducto(idProducto);
        BigDecimal saldoAnterior = historial.isEmpty()
                ? BigDecimal.ZERO
                : historial.get(0).getExistenciaNueva();

        BigDecimal saldoNuevo = saldoAnterior.add(cantidad);

        Inventario inv = inventarioRepo
                .findByProductoAndSucursal(producto, sucursal)
                .orElseGet(() -> {
                    Inventario nuevo = new Inventario();
                    nuevo.setProducto(producto);
                    nuevo.setSucursal(sucursal);
                    nuevo.setExistencia(BigDecimal.ZERO);
                    return nuevo;
                });
        inv.setExistencia(saldoNuevo);
        if (precioCompra != null) inv.setCosto(precioCompra);
        if (precioVenta  != null) inv.setPrecioVenta(precioVenta);
        inventarioRepo.save(inv);

        Kardex k = new Kardex();
        k.setProducto(producto);
        k.setSucursal(sucursal);
        k.setTipoMovimiento("Entrada");
        k.setFecha(fechaIngreso != null ? fechaIngreso : LocalDate.now());
        k.setCantidad(cantidad);
        k.setPrecioUnitario(precioCompra != null ? precioCompra : BigDecimal.ZERO);
        k.setPrecioVenta(precioVenta != null ? precioVenta : BigDecimal.ZERO);
        k.setEgreso(BigDecimal.ZERO);
        k.setExistenciaAnterior(saldoAnterior);
        k.setExistenciaNueva(saldoNuevo);
        k.setObservacion(observacion);
        k.setUsuario(usuario);
        kardexRepo.save(k);
    }

    // ─── Registrar egreso ─────────────────────────────────────────
    @Transactional
    public void registrarEgreso(Integer idProducto,
                                Integer idSucursal,
                                BigDecimal egreso,
                                LocalDate fechaEgreso,
                                Integer idUsuario) throws Exception {

        Producto producto = productoRepo.findById(idProducto)
                .orElseThrow(() -> new Exception("Producto no encontrado."));
        Sucursal sucursal = sucursalRepo.findById(idSucursal)
                .orElseThrow(() -> new Exception("Sucursal no encontrada."));
        Usuario  usuario  = usuarioRepo.findById(idUsuario)
                .orElseThrow(() -> new Exception("Usuario no encontrado."));

        List<Kardex> historial = kardexRepo.findByProducto(idProducto);
        BigDecimal saldoAnterior = historial.isEmpty()
                ? BigDecimal.ZERO
                : historial.get(0).getExistenciaNueva();

        if (egreso.compareTo(saldoAnterior) > 0) {
            throw new Exception("El egreso (" + egreso + ") no puede ser mayor al saldo actual (" + saldoAnterior + ").");
        }

        BigDecimal saldoNuevo = saldoAnterior.subtract(egreso);

        Kardex k = new Kardex();
        k.setProducto(producto);
        k.setSucursal(sucursal);
        k.setTipoMovimiento("Salida");
        k.setFecha(fechaEgreso != null ? fechaEgreso : LocalDate.now());
        k.setCantidad(BigDecimal.ZERO);
        k.setPrecioUnitario(BigDecimal.ZERO);
        k.setPrecioVenta(BigDecimal.ZERO);
        k.setEgreso(egreso);
        k.setFechaEgreso(fechaEgreso);
        k.setExistenciaAnterior(saldoAnterior);
        k.setExistenciaNueva(saldoNuevo);
        k.setObservacion("Egreso de productos");
        k.setUsuario(usuario);
        kardexRepo.save(k);

        Inventario inv = inventarioRepo
                .findByProductoAndSucursal(producto, sucursal)
                .orElseThrow(() -> new Exception("Inventario no encontrado."));
        inv.setExistencia(saldoNuevo);
        inventarioRepo.save(inv);
    }

    // ─── Listar kardex ────────────────────────────────────────────
    public List<Kardex> listarKardex(LocalDate fi, LocalDate ff,
                                     Integer idSucursal, boolean esAdmin) {
        if (esAdmin) return kardexRepo.findByPeriodo(fi, ff);
        return kardexRepo.findByPeriodoYSucursal(fi, ff, idSucursal);
    }

    public List<Kardex> kardexPorProducto(Integer idProducto) {
        return kardexRepo.findByProducto(idProducto);
    }

    // ─── Editar movimiento de kardex ──────────────────────────────
    @Transactional
    public void editarKardex(Integer idKardex,
                             String tipoMovimiento,
                             BigDecimal cantidad,
                             BigDecimal precioUnitario,
                             BigDecimal precioVenta,
                             BigDecimal egreso,
                             String observacion) throws Exception {

        Kardex k = kardexRepo.findById(idKardex)
                .orElseThrow(() -> new Exception("Movimiento no encontrado."));
        k.setTipoMovimiento(tipoMovimiento);
        k.setCantidad(cantidad != null ? cantidad : BigDecimal.ZERO);
        k.setPrecioUnitario(precioUnitario != null ? precioUnitario : BigDecimal.ZERO);
        k.setPrecioVenta(precioVenta != null ? precioVenta : BigDecimal.ZERO);
        k.setEgreso(egreso != null ? egreso : BigDecimal.ZERO);
        k.setObservacion(observacion);
        kardexRepo.save(k);
    }

    // ─── Eliminar movimiento de kardex ────────────────────────────
    @Transactional
    public void eliminarKardex(Integer idKardex) throws Exception {
        Kardex k = kardexRepo.findById(idKardex)
                .orElseThrow(() -> new Exception("Movimiento no encontrado."));
        kardexRepo.delete(k);
    }
}