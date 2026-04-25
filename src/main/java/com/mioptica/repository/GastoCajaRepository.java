package com.mioptica.repository;

import com.mioptica.model.GastoCaja;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface GastoCajaRepository extends JpaRepository<GastoCaja, Integer> {

    // Gastos del día por sucursal
    @Query("SELECT g FROM GastoCaja g " +
           "WHERE g.fecha = :fecha " +
           "AND g.sucursal.idSucursal = :idSuc " +
           "ORDER BY g.idGasto DESC")
    List<GastoCaja> findByFechaYSucursal(@Param("fecha") LocalDate fecha,
                                          @Param("idSuc") Integer idSucursal);

    // Total de gastos del día por sucursal
    @Query("SELECT COALESCE(SUM(g.monto), 0) FROM GastoCaja g " +
           "WHERE g.fecha = :fecha " +
           "AND g.sucursal.idSucursal = :idSuc")
    BigDecimal totalGastosDia(@Param("fecha") LocalDate fecha,
                               @Param("idSuc") Integer idSucursal);
}
