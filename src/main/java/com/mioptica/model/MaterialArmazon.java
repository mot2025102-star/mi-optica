package com.mioptica.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Entity
@Table(name = "materiales_armazon")
public class MaterialArmazon {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_material_armazon")
    private Integer idMaterialArmazon;

    @Column(name = "nombre", nullable = false, unique = true, length = 80)
    private String nombre;
}
