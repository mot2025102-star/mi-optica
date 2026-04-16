package com.mioptica.repository;

import com.mioptica.model.OrdenLaboratorio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface OrdenLaboratorioRepository extends JpaRepository<OrdenLaboratorio, Integer> {

    // Última orden para generar el número correlativo
    @Query("SELECT MAX(o.idOrden) FROM OrdenLaboratorio o")
    Optional<Integer> findMaxId();

    // Listado filtrado por sucursal y rango de fechas
    @Query("""
        SELECT o FROM OrdenLaboratorio o
        WHERE o.fechaEmision BETWEEN :fi AND :ff
          AND (:idSuc IS NULL OR o.sucursal.idSucursal = :idSuc)
        ORDER BY o.fechaEmision DESC, o.idOrden DESC
    """)
    List<OrdenLaboratorio> findByPeriodoYSucursal(
            @Param("fi")    LocalDate fi,
            @Param("ff")    LocalDate ff,
            @Param("idSuc") Integer   idSuc
    );

    // Todas sin filtro de sucursal (para admin)
    @Query("""
        SELECT o FROM OrdenLaboratorio o
        WHERE o.fechaEmision BETWEEN :fi AND :ff
        ORDER BY o.fechaEmision DESC, o.idOrden DESC
    """)
    List<OrdenLaboratorio> findByPeriodo(
            @Param("fi") LocalDate fi,
            @Param("ff") LocalDate ff
    );
}
