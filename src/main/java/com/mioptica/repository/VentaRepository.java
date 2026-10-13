package com.mioptica.repository;

import com.mioptica.model.Inventario;
import com.mioptica.model.Venta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface VentaRepository extends JpaRepository<Venta, Integer> {

    Optional<Venta> findByNumeroFactura(String numeroFactura);

    // Ventas por rango de fechas y sucursal
    @Query("SELECT v FROM Venta v WHERE v.fecha BETWEEN :fi AND :ff AND v.sucursal.idSucursal = :idSuc ORDER BY v.fecha DESC, v.idVenta DESC")
    List<Venta> findByPeriodoYSucursal(@Param("fi") LocalDate fi,
                                       @Param("ff") LocalDate ff,
                                       @Param("idSuc") Integer idSuc);

    // Ventas por rango de fechas (todas las sucursales — para admin)
    @Query("SELECT v FROM Venta v WHERE v.fecha BETWEEN :fi AND :ff ORDER BY v.fecha DESC, v.idVenta DESC")
    List<Venta> findByPeriodo(@Param("fi") LocalDate fi, @Param("ff") LocalDate ff);

    // Total vendido hoy (para stats del dashboard)
    @Query("SELECT COALESCE(SUM(v.total), 0) FROM Venta v WHERE v.fecha = :hoy AND v.estado != 'Anulada' AND (:idSuc IS NULL OR :idSuc = 0 OR v.sucursal.idSucursal = :idSuc)")
    BigDecimal totalHoy(@Param("hoy") LocalDate hoy, @Param("idSuc") Integer idSuc);

    // Cantidad de facturas hoy
    @Query("SELECT COUNT(v) FROM Venta v WHERE v.fecha = :hoy AND v.estado != 'Anulada' AND (:idSuc IS NULL OR :idSuc = 0 OR v.sucursal.idSucursal = :idSuc)")
    long countHoy(@Param("hoy") LocalDate hoy, @Param("idSuc") Integer idSuc);
    // ─── Saldos pendientes (para módulo Saldos y Entregas) ───────────────────

    // Ventas con saldo pendiente > 0 — todas las sucursales (admin)
    @Query("SELECT v FROM Venta v LEFT JOIN FETCH v.cliente " +
           "WHERE v.saldoPendiente > 0 AND v.estado != 'Anulada' " +
           "ORDER BY v.saldoPendiente DESC, v.fecha ASC")
    List<Venta> findConSaldoPendiente();

    // Ventas con saldo pendiente > 0 — por sucursal
    @Query("SELECT v FROM Venta v LEFT JOIN FETCH v.cliente " +
           "WHERE v.saldoPendiente > 0 AND v.estado != 'Anulada' " +
           "AND v.sucursal.idSucursal = :idSuc " +
           "ORDER BY v.saldoPendiente DESC, v.fecha ASC")
    List<Venta> findConSaldoPendienteBySucursal(@Param("idSuc") Integer idSuc);

    // ─── Entregas pendientes (para módulo Saldos y Entregas) ─────────────────

    // Ventas con fecha de entrega asignada y no marcadas como Entregadas — todas las sucursales (admin)
    @Query("SELECT v FROM Venta v LEFT JOIN FETCH v.cliente " +
           "WHERE v.fechaEntrega IS NOT NULL AND v.estado != 'Anulada' " +
           "AND (v.estadoEntrega IS NULL OR v.estadoEntrega != 'Entregado') " +
           "ORDER BY v.fechaEntrega ASC")
    List<Venta> findEntregasPendientes();

    // Ventas con fecha de entrega asignada y no marcadas como Entregadas — por sucursal
    @Query("SELECT v FROM Venta v LEFT JOIN FETCH v.cliente " +
           "WHERE v.fechaEntrega IS NOT NULL AND v.estado != 'Anulada' " +
           "AND (v.estadoEntrega IS NULL OR v.estadoEntrega != 'Entregado') " +
           "AND v.sucursal.idSucursal = :idSuc " +
           "ORDER BY v.fechaEntrega ASC")
    List<Venta> findEntregasPendientesBySucursal(@Param("idSuc") Integer idSuc);

    // Detalle de ventas por cliente (para reporte Excel)
@Query("SELECT v.cliente.nombre, v.fecha, v.numeroFactura, " +
       "d.producto.idProducto, d.producto.detalle, " +
       "d.precioUnitario, d.cantidad, d.subtotal " +
       "FROM DetalleVenta d JOIN d.venta v " +
       "WHERE v.fecha BETWEEN :fi AND :ff " +
       "AND v.estado != 'Anulada' " +
       "AND (:idSuc = 0 OR v.sucursal.idSucursal = :idSuc) " +
       "ORDER BY v.cliente.nombre ASC, v.fecha ASC, v.idVenta ASC")
List<Object[]> detalleVentasPorCliente(@Param("fi") LocalDate fi,
                                        @Param("ff") LocalDate ff,
                                        @Param("idSuc") Integer idSuc);
}
