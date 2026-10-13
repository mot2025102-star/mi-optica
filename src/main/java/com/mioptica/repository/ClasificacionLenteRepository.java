package com.mioptica.repository;

import com.mioptica.model.ClasificacionLente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ClasificacionLenteRepository extends JpaRepository<ClasificacionLente, Integer> {
    Optional<ClasificacionLente> findByNombre(String nombre);
}
