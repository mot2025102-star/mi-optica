package com.mioptica.repository;

import com.mioptica.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Integer> {

    Optional<Usuario> findByUsername(String username);

    Optional<Usuario> findByUsernameAndActivoTrue(String username);

    List<Usuario> findAllByOrderByActivoDescNombreCompletoAsc();

    @Query("SELECT u FROM Usuario u WHERE u.activo = true AND u.sucursal.idSucursal = :idSucursal")
    List<Usuario> findActivosBySucursal(@Param("idSucursal") Integer idSucursal);
}