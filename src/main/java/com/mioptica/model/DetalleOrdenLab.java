package com.mioptica.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Entity
@Table(name = "detalle_orden_lab")
public class DetalleOrdenLab {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_detalle")
    private Integer idDetalle;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_orden", nullable = false)
    private OrdenLaboratorio orden;

    @Column(name = "codigo",      length = 30)  private String codigo;
    @Column(name = "cantidad")                  private Integer cantidad = 1;
    @Column(name = "descripcion", length = 200) private String descripcion;
    @Column(name = "material",    length = 100) private String material;
    @Column(name = "tratamiento", length = 100) private String tratamiento;
    @Column(name = "color_tinte", length = 100) private String colorTinte;
}