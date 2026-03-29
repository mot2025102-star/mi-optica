package com.mioptica.repository;

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
    @Query("SELECT COALESCE(SUM(v.total), 0) FROM Venta v WHERE v.fecha = :hoy AND v.estado != 'Anulada' AND v.sucursal.idSucursal = :idSuc")
    BigDecimal totalHoy(@Param("hoy") LocalDate hoy, @Param("idSuc") Integer idSuc);

    // Cantidad de facturas hoy
    @Query("SELECT COUNT(v) FROM Venta v WHERE v.fecha = :hoy AND v.estado != 'Anulada' AND v.sucursal.idSucursal = :idSuc")
    long countHoy(@Param("hoy") LocalDate hoy, @Param("idSuc") Integer idSuc);
}
