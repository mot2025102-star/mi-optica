package com.mioptica.repository;
 
import com.mioptica.model.Tipo_lente;
import org.springframework.data.jpa.repository.JpaRepository;
 
import java.util.List;
 
public interface Tipo_lenteRepository extends JpaRepository<Tipo_lente, Integer> {
    List<Tipo_lente> findAllByOrderByNombreAsc();
}