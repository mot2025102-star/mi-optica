package com.mioptica.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@Entity
@Table(name = "productos")
public class Producto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_producto")
    private Integer idProducto;

    @NotBlank(message = "El código es obligatorio")
    @Column(name = "codigo", nullable = false, unique = true, length = 50)
    private String codigo;

    @NotBlank(message = "El nombre del producto es obligatorio")
    @Column(name = "detalle", nullable = false, length = 200)
    private String detalle;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_categoria")
    private Categoria categoria;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_marca")
    private Marca marca;

    @Column(name = "descripcion", columnDefinition = "TEXT")
    private String descripcion;

    @Column(name = "unidad_medida", length = 30)
    private String unidadMedida = "Unidad";

    @Column(name = "activo", nullable = false)
    private Boolean activo = true;

    // ─── Tipo de producto ─────────────────────────────────────────
    /** GENERAL | ARMAZON | LENTE | LIMPIEZA | ACCESORIO */
    @Column(name = "tipo_producto", length = 20)
    private String tipoProducto = "GENERAL";

    @Column(name = "color", length = 50)
    private String color;

    @Column(name = "modelo", length = 80)
    private String modelo;

    // ─── Clasificación de Armazón ─────────────────────────────────
    /** ACERO_INOXIDABLE | TITANIUM | ALUMINIO | MONEL | ACETATO | TR90 | INYECTADO */
    @Column(name = "material_armazon", length = 30)
    private String materialArmazon;

    /** COMPLETO | RANURADO | AEREO */
    @Column(name = "familia_armazon", length = 20)
    private String familiaArmazon;

    /** CUADRADO | OVALADO | RECTANGULAR | AVIADOR | CAT_EYES */
    @Column(name = "forma_armazon", length = 20)
    private String formaArmazon;

    /** CABALLERO | DAMA | NIÑO | UNISEX */
    @Column(name = "segmento_armazon", length = 20)
    private String segmentoArmazon;

    /** BASICA | MEDIA | ALTA */
    @Column(name = "gama_armazon", length = 15)
    private String gamaArmazon;

    // ─── Medidas del Armazón (mm) ─────────────────────────────────
    @Column(name = "med_horizontal", precision = 5, scale = 1)
    private BigDecimal medHorizontal;

    @Column(name = "med_vertical", precision = 5, scale = 1)
    private BigDecimal medVertical;

    @Column(name = "med_diagonal", precision = 5, scale = 1)
    private BigDecimal medDiagonal;

    @Column(name = "med_puente", precision = 5, scale = 1)
    private BigDecimal medPuente;

    @Column(name = "med_varrilla", precision = 5, scale = 1)
    private BigDecimal medVarrilla;

    // ─── Relaciones solo para LENTE ───────────────────────────────
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_tipo_lente")
    private Tipo_lente tipoLente;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_material_lente")
    private Material_lente materialLente;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_tratamiento_lente")
    private Tratamiento_lente tratamientoLente;

    // ─── Helpers ──────────────────────────────────────────────────
    public boolean esLente()    { return "LENTE".equals(tipoProducto); }
    public boolean esArmazon()  { return "ARMAZON".equals(tipoProducto); }
    public boolean esGeneral()  { return tipoProducto == null || "GENERAL".equals(tipoProducto); }
}