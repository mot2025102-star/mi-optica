package com.mioptica.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@Entity
@Table(name = "kardex")
public class Kardex {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_kardex")
    private Integer idKardex;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_producto", nullable = false)
    private Producto producto;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_sucursal", nullable = false)
    private Sucursal sucursal;

    @Column(name = "tipo_movimiento", nullable = false, length = 30)
    private String tipoMovimiento; // Entrada | Salida | Traslado_Entrada | Traslado_Salida | Ajuste

    @Column(name = "referencia", length = 50)
    private String referencia;

    // ─── Ingreso ──────────────────────────────────────────────────
    @Column(name = "fecha", nullable = false)
    private LocalDate fecha; // fecha de ingreso

    @Column(name = "cantidad", nullable = false, precision = 10, scale = 2)
    private BigDecimal cantidad = BigDecimal.ZERO; // unidades que ingresaron

    @Column(name = "precio_unitario", precision = 10, scale = 2)
    private BigDecimal precioUnitario = BigDecimal.ZERO; // precio compra

    // ─── Egreso ───────────────────────────────────────────────────
    @Column(name = "egreso", precision = 10, scale = 2)
    private BigDecimal egreso = BigDecimal.ZERO; // unidades que salieron

    @Column(name = "fecha_egreso")
    private LocalDate fechaEgreso; // fecha en que salieron

    @Column(name = "precio_venta", precision = 10, scale = 2)
    private BigDecimal precioVenta = BigDecimal.ZERO; // precio venta por unidad
    
    // ─── Saldo ────────────────────────────────────────────────────
    @Column(name = "existencia_anterior", precision = 10, scale = 2)
    private BigDecimal existenciaAnterior = BigDecimal.ZERO;

    @Column(name = "existencia_nueva", precision = 10, scale = 2)
    private BigDecimal existenciaNueva = BigDecimal.ZERO; // saldo acumulativo

    @Column(name = "observacion", length = 200)
    private String observacion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario")
    private Usuario usuario;
}
