package com.mioptica.repository;

import com.mioptica.model.ReciboCaja;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface ReciboCajaRepository extends JpaRepository<ReciboCaja, Integer> {

    @Query("SELECT r FROM ReciboCaja r WHERE r.fecha = :fecha AND r.sucursal.idSucursal = :idSucursal")
    List<ReciboCaja> findByFechaYSucursal(@Param("fecha") LocalDate fecha, @Param("idSucursal") Integer idSucursal);

    @Query("SELECT r.formaPago, SUM(r.monto) FROM ReciboCaja r WHERE r.fecha BETWEEN :fechaInicio AND :fechaFin AND r.sucursal.idSucursal = :idSucursal GROUP BY r.formaPago")
List<Object[]> totalPorFormaPago(@Param("fechaInicio") LocalDate fechaInicio, @Param("fechaFin") LocalDate fechaFin, @Param("idSucursal") Integer idSucursal);
}