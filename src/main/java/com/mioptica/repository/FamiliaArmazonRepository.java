package com.mioptica.repository;

import com.mioptica.model.FamiliaArmazon;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FamiliaArmazonRepository extends JpaRepository<FamiliaArmazon, Integer> {
    List<FamiliaArmazon> findAllByOrderByNombreAsc();
}
