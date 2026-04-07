package com.mioptica.model;
 
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.NoArgsConstructor;
 
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
 
    // ─── Campos nuevos ────────────────────────────────────────────
 
    /** GENERAL | ARMAZON | LENTE | LIMPIEZA | ACCESORIO */
    @Column(name = "tipo_producto", length = 20)
    private String tipoProducto = "GENERAL";
 
    @Column(name = "color", length = 50)
    private String color;
 
    @Column(name = "modelo", length = 80)
    private String modelo;
 
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
 
    // ─── Helper ───────────────────────────────────────────────────
    public boolean esLente()    { return "LENTE".equals(tipoProducto); }
    public boolean esArmazon()  { return "ARMAZON".equals(tipoProducto); }
    public boolean esGeneral()  { return tipoProducto == null || "GENERAL".equals(tipoProducto); }
}