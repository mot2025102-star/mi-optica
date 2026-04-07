package com.mioptica.repository;

import com.mioptica.model.ReciboCaja;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ReciboCajaRepository extends JpaRepository<ReciboCaja, Integer> { }
