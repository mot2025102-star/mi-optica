package com.mioptica.repository;

import com.mioptica.model.MaterialArmazon;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MaterialArmazonRepository extends JpaRepository<MaterialArmazon, Integer> {
    List<MaterialArmazon> findAllByOrderByNombreAsc();
}
