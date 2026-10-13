package com.mioptica.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "notificaciones")
@Data
@NoArgsConstructor
public class Notificacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String tipo;      // STOCK, ENTREGA, COBRO, LABORATORIO
    private String severidad; // CRITICO, ATENCION, INFO
    private String titulo;
    private String mensaje;
    private String url;       // A dónde lleva al hacer clic
    private boolean leida = false;
    
    @Column(name = "creada")
    private LocalDateTime creada = LocalDateTime.now();

    public Notificacion(String tipo, String severidad, String titulo, String mensaje, String url) {
        this.tipo = tipo;
        this.severidad = severidad;
        this.titulo = titulo;
        this.mensaje = mensaje;
        this.url = url;
    }
}
