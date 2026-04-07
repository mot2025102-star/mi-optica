package com.mioptica.repository;

import com.mioptica.model.Producto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductoRepository extends JpaRepository<Producto, Integer> {

    Optional<Producto> findByCodigo(String codigo);

    List<Producto> findByActivoTrueOrderByDetalleAsc();

    @Query("SELECT p FROM Producto p WHERE p.activo = true AND " +
           "(LOWER(p.codigo) LIKE LOWER(CONCAT('%',:q,'%')) OR " +
           " LOWER(p.detalle) LIKE LOWER(CONCAT('%',:q,'%')))")
    List<Producto> buscar(@Param("q") String q);
}