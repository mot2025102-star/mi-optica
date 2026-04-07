package com.mioptica.model;
 
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
 
@Data
@NoArgsConstructor
@Entity
@Table(name = "materiales_lente")
public class Material_lente {
 
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_material")
    private Integer idMaterial;
 
    @Column(name = "nombre", nullable = false, unique = true, length = 80)
    private String nombre;
}
