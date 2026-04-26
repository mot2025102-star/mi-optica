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
 
    // ─── Relaciones para LENTE ────────────────────────────────────
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_tipo_lente")
    private Tipo_lente tipoLente;
 
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_material_lente")
    private Material_lente materialLente;
 
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_tratamiento_lente")
    private Tratamiento_lente tratamientoLente;
 
    // ─── NUEVOS: Campos específicos de Lente ─────────────────────
 
    /**
     * Clasificación general del lente:
     * OFTALMICO_TERMINADO | OFTALMICO_PROCESADO |
     * CONTACTO_DESECHABLE | CONTACTO_GAS_PERMEABLE | CONTACTO_HIBRIDO
     */
    @Column(name = "clasificacion_lente", length = 30)
    private String clasificacionLente;
 
    /**
     * Sub-familia / tratamiento adicional:
     * UV | ANTIREFLEJO | FILTRO_LUZ_AZUL | FOTOCROMÁTICO |
     * AR_FILTRO_AZUL | AR_FOTOCROMÁTICO | AR_FILTRO_AZUL_FOTOCROMÁTICO
     */
    @Column(name = "sub_familia_lente", length = 40)
    private String subFamiliaLente;
 
    /**
     * Rango de graduación: BAJA | MEDIA | ALTA
     */
    @Column(name = "rango_lente", length = 10)
    private String rangoLente;
 
    /**
     * Proveedor:
     * CASA_OPTICA | SERDEL | LAB_CILIAR | SOLUCION_OPTICA | TOPEX | ARCO_IRIS
     */
    @Column(name = "proveedor_lente", length = 20)
    private String proveedorLente;
 
    /**
     * Código interno del lente (ej: LNT-0.25-0.50 PC/TR/AR)
     */
    @Column(name = "codigo_interno_lente", length = 80)
    private String codigoInternoLente;
 
    /**
     * Precio de costo del lente
     */
    @Column(name = "costo_lente", precision = 10, scale = 2)
    private BigDecimal costoLente;
 
    /**
     * Porcentaje de utilidad (ej: 30.00 para 30%)
     */
    @Column(name = "utilidad_lente", precision = 5, scale = 2)
    private BigDecimal utilidadLente;
 
    /**
     * Porcentaje de IVA aplicado (ej: 12.00 para 12%)
     */
    @Column(name = "iva_lente", precision = 5, scale = 2)
    private BigDecimal ivaLente;
 
    /**
     * Precio de venta calculado
     */
    @Column(name = "precio_venta_lente", precision = 10, scale = 2)
    private BigDecimal precioVentaLente;
 
    /**
     * Stock actual del lente
     */
    @Column(name = "stock_lente")
    private Integer stockLente;
 
    // ─── Helpers ──────────────────────────────────────────────────
    public boolean esLente()    { return "LENTE".equals(tipoProducto); }
    public boolean esArmazon()  { return "ARMAZON".equals(tipoProducto); }
    public boolean esGeneral()  { return tipoProducto == null || "GENERAL".equals(tipoProducto); }
}
 