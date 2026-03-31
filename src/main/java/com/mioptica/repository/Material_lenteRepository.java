package com.mioptica.repository;
 
import com.mioptica.model.Material_lente;
import org.springframework.data.jpa.repository.JpaRepository;
 
import java.util.List;
 
public interface Material_lenteRepository extends JpaRepository<Material_lente, Integer> {
    List<Material_lente> findAllByOrderByNombreAsc();
}