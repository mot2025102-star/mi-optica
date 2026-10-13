package com.mioptica.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Entity
@Table(name = "formas_armazon")
public class FormaArmazon {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_forma_armazon")
    private Integer idFormaArmazon;

    @Column(name = "nombre", nullable = false, unique = true, length = 80)
    private String nombre;
}
