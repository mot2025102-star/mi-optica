package com.mioptica.repository;
 
import com.mioptica.model.Precio_lente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
 
import java.util.List;
import java.util.Optional;
 
public interface Precio_lenteRepository extends JpaRepository<Precio_lente, Integer> {
 
    List<Precio_lente> findAllByOrderByTipoNombreAscMaterialNombreAsc();
 
    @Query("""
        SELECT p FROM Precio_lente p
        WHERE p.tipo.idTipo                     = :idTipo
          AND p.material.idMaterial             = :idMaterial
          AND p.tratamiento.idTratamiento       = :idTratamiento
    """)
    Optional<Precio_lente> findByCombinacion(
            @Param("idTipo")         Integer idTipo,
            @Param("idMaterial")     Integer idMaterial,
            @Param("idTratamiento")  Integer idTratamiento
    );
}