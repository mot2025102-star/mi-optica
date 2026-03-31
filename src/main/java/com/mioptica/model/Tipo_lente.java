package com.mioptica.model;
 
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
 
@Data
@NoArgsConstructor
@Entity
@Table(name = "tipos_lente")
public class Tipo_lente {
 
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_tipo")
    private Integer idTipo;
 
    @Column(name = "nombre", nullable = false, unique = true, length = 80)
    private String nombre;
}
