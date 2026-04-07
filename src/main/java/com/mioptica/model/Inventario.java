package com.mioptica.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@Entity
@Table(name = "inventario",
       uniqueConstraints = @UniqueConstraint(columnNames = {"id_producto", "id_sucursal"}))
public class Inventario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_inventario")
    private Integer idInventario;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_producto", nullable = false)
    private Producto producto;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_sucursal", nullable = false)
    private Sucursal sucursal;

    @Column(name = "existencia", nullable = false, precision = 10, scale = 2)
    private BigDecimal existencia = BigDecimal.ZERO;

    @Column(name = "costo", precision = 10, scale = 2)
    private BigDecimal costo = BigDecimal.ZERO;

    @Column(name = "precio_venta", precision = 10, scale = 2)
    private BigDecimal precioVenta = BigDecimal.ZERO;
}
