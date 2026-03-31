package com.mioptica.model;
 
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
 
import java.math.BigDecimal;
 
@Data
@NoArgsConstructor
@Entity
@Table(name = "precios_lente")
public class Precio_lente {
 
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_precio")
    private Integer idPrecio;
 
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_tipo", nullable = false)
    private Tipo_lente tipo;
 
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_material", nullable = false)
    private Material_lente material;
 
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_tratamiento", nullable = false)
    private Tratamiento_lente tratamiento;
 
    @Column(name = "costo", nullable = false, precision = 10, scale = 2)
    private BigDecimal costo = BigDecimal.ZERO;
 
    @Column(name = "precio_venta", nullable = false, precision = 10, scale = 2)
    private BigDecimal precioVenta = BigDecimal.ZERO;
}