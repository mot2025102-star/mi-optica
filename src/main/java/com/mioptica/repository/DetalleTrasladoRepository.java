package com.mioptica.repository;

import com.mioptica.model.DetalleTraslado;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DetalleTrasladoRepository extends JpaRepository<DetalleTraslado, Integer> {
}

