package com.mioptica.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@Entity
@Table(name = "ordenes_laboratorio")
public class OrdenLaboratorio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_orden")
    private Integer idOrden;

    @Column(name = "numero_orden", unique = true, length = 20)
    private String numeroOrden;   // e.g. "OL-000152"

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_sucursal", nullable = false)
    private Sucursal sucursal;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_usuario", nullable = false)
    private Usuario usuario;      // quien crea la orden (vendedor/optometrista)

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_cliente")
    private Cliente cliente;      // puede ser null (CF)

    // ── Datos Clínicos OD ──────────────────────────────────────────
    @Column(name = "od_esfera",   length = 10) private String odEsfera;
    @Column(name = "od_cilindro", length = 10) private String odCilindro;
    @Column(name = "od_eje",      length = 10) private String odEje;
    @Column(name = "od_add",      length = 10) private String odAdd;
    @Column(name = "od_dip",      length = 10) private String odDip;
    @Column(name = "od_ndpod",    length = 10) private String odNdpod;
    @Column(name = "od_ndpoi",    length = 10) private String odNdpoi;
    @Column(name = "od_altura",   length = 10) private String odAltura;

    // ── Datos Clínicos OI ──────────────────────────────────────────
    @Column(name = "oi_esfera",   length = 10) private String oiEsfera;
    @Column(name = "oi_cilindro", length = 10) private String oiCilindro;
    @Column(name = "oi_eje",      length = 10) private String oiEje;
    @Column(name = "oi_add",      length = 10) private String oiAdd;
    @Column(name = "oi_dip",      length = 10) private String oiDip;
    @Column(name = "oi_ndpod",    length = 10) private String oiNdpod;
    @Column(name = "oi_ndpoi",    length = 10) private String oiNdpoi;
    @Column(name = "oi_altura",   length = 10) private String oiAltura;

    // ── Medidas adicionales ────────────────────────────────────────
    @Column(name = "pantoscopico", length = 10) private String pantoscopico;
    @Column(name = "vertex",       length = 10) private String vertex;
    @Column(name = "panoramico",   length = 10) private String panoramico;

    // ── Proceso y Entrega ──────────────────────────────────────────
    @Column(name = "fecha_emision",    nullable = false) private LocalDate fechaEmision;
    @Column(name = "fecha_envio_lab")                   private LocalDate fechaEnvioLab;
    @Column(name = "fecha_entrega_estimada")            private LocalDate fechaEntregaEstimada;

    @Column(name = "estado", length = 30)
    private String estado = "Pendiente";  // Pendiente | Enviado | Entregado | Cancelado

    @Column(name = "observaciones", columnDefinition = "TEXT")
    private String observaciones;

    @OneToMany(mappedBy = "orden", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DetalleOrdenLab> productos = new ArrayList<>();

    // ── Origen de la orden ─────────────────────────────────────────
    @Column(name = "origen", length = 30)
    private String origen = "MANUAL";
    // Valores: FICHA_INTERNA | RECETA_EXTERNA | MANUAL

    @Column(name = "id_ficha")
    private Integer idFicha;  // FK a fichas_clinicas (null si no viene de ficha)

    @Column(name = "nota_origen", columnDefinition = "TEXT")
    private String notaOrigen;  // "Graduación dada por el cliente" si es externa
}
