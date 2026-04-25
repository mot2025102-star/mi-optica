package com.mioptica.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@Entity
@Table(name = "traslados")
public class Traslado {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_traslado")
    private Integer idTraslado;

    @Column(name = "numero_registro", unique = true, length = 20)
    private String numeroRegistro;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_sucursal_orig", nullable = false)
    private Sucursal sucursalOrigen;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_sucursal_dest", nullable = false)
    private Sucursal sucursalDestino;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_usuario", nullable = false)
    private Usuario usuario;

    @Column(name = "fecha", nullable = false)
    private LocalDate fecha = LocalDate.now();

    @Column(name = "nota", columnDefinition = "TEXT")
    private String nota;

    @Column(name = "estado", length = 20)
    private String estado = "Pendiente";
}

