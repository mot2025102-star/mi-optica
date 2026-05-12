package com.mioptica.repository;

import com.mioptica.model.Proveedor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ProveedorRepository extends JpaRepository<Proveedor, Integer> {

    List<Proveedor> findByActivoTrueOrderByNombreAsc();

    @Query("SELECT p FROM Proveedor p WHERE " +
           "LOWER(p.nombre) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(p.empresa) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(p.nit) LIKE LOWER(CONCAT('%', :query, '%'))")
    List<Proveedor> searchByQuery(@Param("query") String query);
}
