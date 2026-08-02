package com.mioptica.repository;

import com.mioptica.model.Venta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface ReporteRepository extends JpaRepository<Venta, Integer> {

    @Query("SELECT d.producto.detalle, SUM(d.cantidad), SUM(d.subtotal), " +
           "SUM(COALESCE(i.costo, 0) * d.cantidad) " +
           "FROM DetalleVenta d JOIN d.venta v " +
           "LEFT JOIN Inventario i ON i.producto = d.producto AND i.sucursal = v.sucursal " +
           "WHERE v.fecha BETWEEN :fi AND :ff " +
           "AND v.estado != 'Anulada' " +
           "AND (:idSuc = 0 OR v.sucursal.idSucursal = :idSuc) " +
           "GROUP BY d.producto.idProducto, d.producto.detalle " +
           "ORDER BY SUM(d.subtotal) DESC")
    List<Object[]> topProductos(@Param("fi") LocalDate fi,
                                @Param("ff") LocalDate ff,
                                @Param("idSuc") Integer idSuc);

    // ── Ventas por vendedor ────────────────────────────────────────
    @Query("SELECT v.usuario.nombreCompleto, COUNT(v), SUM(v.total) " +
           "FROM Venta v " +
           "WHERE v.fecha BETWEEN :fi AND :ff " +
           "AND v.estado != 'Anulada' " +
           "AND (:idSuc = 0 OR v.sucursal.idSucursal = :idSuc) " +
           "GROUP BY v.usuario.idUsuario, v.usuario.nombreCompleto " +
           "ORDER BY SUM(v.total) DESC")
    List<Object[]> ventasPorVendedor(@Param("fi") LocalDate fi,
                                     @Param("ff") LocalDate ff,
                                     @Param("idSuc") Integer idSuc);

    // ── Ventas por categoría ───────────────────────────────────────
    @Query("SELECT COALESCE(d.producto.tipoProducto,'GENERAL'), SUM(d.cantidad), SUM(d.subtotal) " +
           "FROM DetalleVenta d " +
           "WHERE d.venta.fecha BETWEEN :fi AND :ff " +
           "AND d.venta.estado != 'Anulada' " +
           "AND (:idSuc = 0 OR d.venta.sucursal.idSucursal = :idSuc) " +
           "GROUP BY d.producto.tipoProducto " +
           "ORDER BY SUM(d.subtotal) DESC")
    List<Object[]> ventasPorCategoria(@Param("fi") LocalDate fi,
                                      @Param("ff") LocalDate ff,
                                      @Param("idSuc") Integer idSuc);

    // ── Ventas por día (para gráfica de línea) ─────────────────────
    @Query("SELECT v.fecha, COUNT(v), SUM(v.total) " +
           "FROM Venta v " +
           "WHERE v.fecha BETWEEN :fi AND :ff " +
           "AND v.estado != 'Anulada' " +
           "AND (:idSuc = 0 OR v.sucursal.idSucursal = :idSuc) " +
           "GROUP BY v.fecha " +
           "ORDER BY v.fecha ASC")
    List<Object[]> ventasPorDia(@Param("fi") LocalDate fi,
                                @Param("ff") LocalDate ff,
                                @Param("idSuc") Integer idSuc);

    // ── Total general del período ──────────────────────────────────
    @Query("SELECT COALESCE(SUM(v.total), 0) FROM Venta v " +
           "WHERE v.fecha BETWEEN :fi AND :ff " +
           "AND v.estado != 'Anulada' " +
           "AND (:idSuc = 0 OR v.sucursal.idSucursal = :idSuc)")
    BigDecimal totalPeriodo(@Param("fi") LocalDate fi,
                            @Param("ff") LocalDate ff,
                            @Param("idSuc") Integer idSuc);

    // ── Conteo facturas período ────────────────────────────────────
    @Query("SELECT COUNT(v) FROM Venta v " +
           "WHERE v.fecha BETWEEN :fi AND :ff " +
           "AND v.estado != 'Anulada' " +
           "AND (:idSuc = 0 OR v.sucursal.idSucursal = :idSuc)")
    Long countPeriodo(@Param("fi") LocalDate fi,
                      @Param("ff") LocalDate ff,
                      @Param("idSuc") Integer idSuc);

    // ── Detalle de ventas (tab Detalle) ────────────────────────────
    @Query("SELECT v.numeroFactura, v.fecha, v.usuario.nombreCompleto, " +
           "COALESCE(d.producto.tipoProducto,'GENERAL'), " +
           "d.producto.detalle, SUM(d.cantidad), SUM(d.subtotal), v.estado, " +
           "SUM(COALESCE(i.costo, 0) * d.cantidad) " +
           "FROM DetalleVenta d JOIN d.venta v " +
           "LEFT JOIN Inventario i ON i.producto = d.producto AND i.sucursal = v.sucursal " +
           "WHERE v.fecha BETWEEN :fi AND :ff " +
           "AND v.estado != 'Anulada' " +
           "AND (:idSuc = 0 OR v.sucursal.idSucursal = :idSuc) " +
           "AND (:idVend = 0 OR v.usuario.idUsuario = :idVend) " +
           "GROUP BY v.idVenta, v.numeroFactura, v.fecha, " +
           "v.usuario.nombreCompleto, d.producto.tipoProducto, " +
           "d.producto.detalle, v.estado " +
           "ORDER BY v.fecha DESC, v.idVenta DESC")
    List<Object[]> detalleVentas(@Param("fi") LocalDate fi,
                                 @Param("ff") LocalDate ff,
                                 @Param("idSuc") Integer idSuc,
                                 @Param("idVend") Integer idVend);

    // ════════════════════════════════════════════════════════════════
    // ANALYTICS QUERIES (nativeQuery para funciones MySQL)
    // ════════════════════════════════════════════════════════════════

    // ── Ventas por mes (año-mes) ───────────────────────────────────
    @Query(value = "SELECT DATE_FORMAT(v.fecha, '%Y-%m') AS mes, " +
                   "COUNT(v.id_venta) AS facturas, SUM(v.total) AS total " +
                   "FROM ventas v " +
                   "WHERE v.fecha BETWEEN :fi AND :ff " +
                   "AND v.estado != 'Anulada' " +
                   "AND (:idSuc = 0 OR v.id_sucursal = :idSuc) " +
                   "GROUP BY DATE_FORMAT(v.fecha, '%Y-%m') " +
                   "ORDER BY mes ASC",
           nativeQuery = true)
    List<Object[]> ventasPorMes(@Param("fi") LocalDate fi,
                                @Param("ff") LocalDate ff,
                                @Param("idSuc") Integer idSuc);

    // ── Ventas por día de la semana (1=Dom … 7=Sáb en MySQL) ──────
    @Query(value = "SELECT DAYOFWEEK(v.fecha) AS dow, SUM(v.total) AS total, COUNT(v.id_venta) AS facturas " +
                   "FROM ventas v " +
                   "WHERE v.fecha BETWEEN :fi AND :ff " +
                   "AND v.estado != 'Anulada' " +
                   "AND (:idSuc = 0 OR v.id_sucursal = :idSuc) " +
                   "GROUP BY DAYOFWEEK(v.fecha) " +
                   "ORDER BY dow ASC",
           nativeQuery = true)
    List<Object[]> ventasPorDiaSemana(@Param("fi") LocalDate fi,
                                      @Param("ff") LocalDate ff,
                                      @Param("idSuc") Integer idSuc);

    // ── Top 10 productos por cantidad ─────────────────────────────
    @Query(value = "SELECT p.detalle, SUM(dv.cantidad) AS total_cant " +
                   "FROM detalle_ventas dv " +
                   "JOIN ventas v ON dv.id_venta = v.id_venta " +
                   "JOIN productos p ON dv.id_producto = p.id_producto " +
                   "WHERE v.fecha BETWEEN :fi AND :ff " +
                   "AND v.estado != 'Anulada' " +
                   "AND (:idSuc = 0 OR v.id_sucursal = :idSuc) " +
                   "GROUP BY p.id_producto, p.detalle " +
                   "ORDER BY total_cant DESC " +
                   "LIMIT 10",
           nativeQuery = true)
    List<Object[]> topProductosPorCantidad(@Param("fi") LocalDate fi,
                                           @Param("ff") LocalDate ff,
                                           @Param("idSuc") Integer idSuc);

    // ── Resumen por sucursal (total y facturas) ────────────────────
    @Query(value = "SELECT s.id_sucursal, s.nombre, " +
                   "COUNT(v.id_venta) AS facturas, COALESCE(SUM(v.total), 0) AS total " +
                   "FROM sucursales s " +
                   "LEFT JOIN ventas v ON v.id_sucursal = s.id_sucursal " +
                   "   AND v.fecha BETWEEN :fi AND :ff AND v.estado != 'Anulada' " +
                   "WHERE s.activo = 1 " +
                   "GROUP BY s.id_sucursal, s.nombre " +
                   "ORDER BY s.nombre ASC",
           nativeQuery = true)
    List<Object[]> resumenPorSucursal(@Param("fi") LocalDate fi,
                                      @Param("ff") LocalDate ff);

    // ── Ventas por día para una sucursal específica ────────────────
    @Query(value = "SELECT v.fecha, SUM(v.total) AS total " +
                   "FROM ventas v " +
                   "WHERE v.fecha BETWEEN :fi AND :ff " +
                   "AND v.estado != 'Anulada' " +
                   "AND v.id_sucursal = :idSuc " +
                   "GROUP BY v.fecha " +
                   "ORDER BY v.fecha ASC",
           nativeQuery = true)
    List<Object[]> ventasPorDiaPorSucursal(@Param("fi") LocalDate fi,
                                           @Param("ff") LocalDate ff,
                                           @Param("idSuc") Integer idSuc);
}