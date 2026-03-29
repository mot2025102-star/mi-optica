package com.mioptica.repository;

import com.mioptica.model.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ClienteRepository extends JpaRepository<Cliente, Integer> {

    Optional<Cliente> findByDpi(String dpi);

    List<Cliente> findByActivoTrueOrderByNombreAsc();

    List<Cliente> findAllByOrderByActivoDescNombreAsc();

    @Query("SELECT c FROM Cliente c WHERE c.activo = true AND " +
           "(LOWER(c.nombre) LIKE LOWER(CONCAT('%',:q,'%')) OR " +
           " c.dpi LIKE CONCAT('%',:q,'%') OR " +
           " c.nit LIKE CONCAT('%',:q,'%') OR " +
           " c.telefono LIKE CONCAT('%',:q,'%'))")
    List<Cliente> buscar(@Param("q") String q);
}
