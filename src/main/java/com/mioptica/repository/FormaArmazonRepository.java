package com.mioptica.repository;

import com.mioptica.model.FormaArmazon;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FormaArmazonRepository extends JpaRepository<FormaArmazon, Integer> {
    List<FormaArmazon> findAllByOrderByNombreAsc();
}
