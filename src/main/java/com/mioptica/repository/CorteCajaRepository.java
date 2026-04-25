package com.mioptica.repository;

import com.mioptica.model.CorteCaja;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface CorteCajaRepository extends JpaRepository<CorteCaja, Integer> {

     // ✅ Usando @Query para evitar depender del nombre del campo
    @Query("SELECT c FROM CorteCaja c WHERE c.sucursal.idSucursal = :idSucursal AND c.fecha = :fecha")
    Optional<CorteCaja> findByIdSucursalAndFecha(
        @Param("idSucursal") Integer idSucursal,
        @Param("fecha") LocalDate fecha
    );

    // ✅ Este ya estaba bien con @Query
    @Query("SELECT c FROM CorteCaja c " +
           "WHERE c.sucursal.idSucursal = :idSuc " +
           "ORDER BY c.fecha DESC")
    List<CorteCaja> findBySucursal(@Param("idSuc") Integer idSucursal);

    List<CorteCaja> findAllByOrderByFechaDesc();
}