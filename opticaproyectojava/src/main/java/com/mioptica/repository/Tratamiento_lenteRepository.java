package com.mioptica.repository;
 
import com.mioptica.model.Tratamiento_lente;
import org.springframework.data.jpa.repository.JpaRepository;
 
import java.util.List;
 
public interface Tratamiento_lenteRepository extends JpaRepository<Tratamiento_lente, Integer> {
    List<Tratamiento_lente> findAllByOrderByNombreAsc();
}