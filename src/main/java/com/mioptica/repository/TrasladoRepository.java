package com.mioptica.repository;

import com.mioptica.model.Traslado;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TrasladoRepository extends JpaRepository<Traslado, Integer> {
}

