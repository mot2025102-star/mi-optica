package com.mioptica.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@Entity
@Table(name = "cortes_caja",
       uniqueConstraints = @UniqueConstraint(columnNames = {"id_sucursal", "fecha"}))
public class CorteCaja {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_corte")
    private Integer idCorte;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_sucursal", nullable = false)
    private Sucursal sucursal;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_usuario", nullable = false)
    private Usuario usuario;

    @Column(name = "fecha", nullable = false)
    private LocalDate fecha;

    // Saldo con el que abre la caja
    @Column(name = "saldo_inicial", nullable = false, precision = 10, scale = 2)
    private BigDecimal saldoInicial = BigDecimal.ZERO;

    // Lo que el cajero contó físicamente al cerrar
    @Column(name = "saldo_fisico", precision = 10, scale = 2)
    private BigDecimal saldoFisico;

    // Total de ventas del día (calculado automáticamente)
    @Column(name = "total_ventas", precision = 10, scale = 2)
    private BigDecimal totalVentas = BigDecimal.ZERO;

    // Total de gastos del día (calculado automáticamente)
    @Column(name = "total_gastos", precision = 10, scale = 2)
    private BigDecimal totalGastos = BigDecimal.ZERO;

    // saldo_inicial + ingresos - gastos
    @Column(name = "saldo_esperado", precision = 10, scale = 2)
    private BigDecimal saldoEsperado = BigDecimal.ZERO;

    // saldo_fisico - saldo_esperado
    @Column(name = "diferencia", precision = 10, scale = 2)
    private BigDecimal diferencia = BigDecimal.ZERO;

    @Column(name = "observacion", columnDefinition = "TEXT")
    private String observacion;

    // false = abierto, true = cerrado
    @Column(name = "cerrado", nullable = false)
    private Boolean cerrado = false;
}

