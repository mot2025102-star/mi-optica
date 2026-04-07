package com.mioptica.model;
 
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
 
@Data
@NoArgsConstructor
@Entity
@Table(name = "tratamientos_lente")
public class Tratamiento_lente {
 
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_tratamiento")
    private Integer idTratamiento;
 
    @Column(name = "nombre", nullable = false, unique = true, length = 80)
    private String nombre;
}