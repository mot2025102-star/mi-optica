package com.mioptica.repository;

import com.mioptica.model.Kardex;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface KardexRepository extends JpaRepository<Kardex, Integer> {

    // Movimientos por período (todas las sucursales)
    @Query("SELECT k FROM Kardex k " +
           "JOIN FETCH k.producto " +
           "JOIN FETCH k.sucursal " +
           "WHERE k.fecha BETWEEN :fi AND :ff " +
           "ORDER BY k.fecha DESC, k.idKardex DESC")
    List<Kardex> findByPeriodo(@Param("fi") LocalDate fi,
                               @Param("ff") LocalDate ff);

    // Movimientos por período filtrando sucursal
    @Query("SELECT k FROM Kardex k " +
           "JOIN FETCH k.producto " +
           "JOIN FETCH k.sucursal " +
           "WHERE k.fecha BETWEEN :fi AND :ff " +
           "AND k.sucursal.idSucursal = :idSuc " +
           "ORDER BY k.fecha DESC, k.idKardex DESC")
    List<Kardex> findByPeriodoYSucursal(@Param("fi")    LocalDate fi,
                                        @Param("ff")    LocalDate ff,
                                        @Param("idSuc") Integer   idSuc);

    // Últimos movimientos de un producto (para modal rápido)
    @Query("SELECT k FROM Kardex k " +
           "JOIN FETCH k.sucursal " +
           "WHERE k.producto.idProducto = :idProd " +
           "ORDER BY k.fecha DESC, k.idKardex DESC")
    List<Kardex> findByProducto(@Param("idProd") Integer idProd);
}