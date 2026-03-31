package com.mioptica.repository;

import com.mioptica.model.Correlativo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CorrelativoRepository extends JpaRepository<Correlativo, Integer> {

    @Query("SELECT c FROM Correlativo c WHERE c.sucursal.idSucursal = :idSuc AND c.tipo = :tipo")
    Optional<Correlativo> findBySucursalAndTipo(@Param("idSuc") Integer idSuc,
                                                @Param("tipo")  String tipo);
}
