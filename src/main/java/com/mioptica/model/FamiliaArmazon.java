package com.mioptica.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Entity
@Table(name = "familias_armazon")
public class FamiliaArmazon {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_familia_armazon")
    private Integer idFamiliaArmazon;

    @Column(name = "nombre", nullable = false, unique = true, length = 80)
    private String nombre;
}
