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
 
    @Column(name = "saldo_inicial", nullable = false, precision = 10, scale = 2)
    private BigDecimal saldoInicial = BigDecimal.ZERO;
 
    @Column(name = "saldo_fisico", precision = 10, scale = 2)
    private BigDecimal saldoFisico;
 
    @Column(name = "total_ventas", precision = 10, scale = 2)
    private BigDecimal totalVentas = BigDecimal.ZERO;
 
    @Column(name = "total_gastos", precision = 10, scale = 2)
    private BigDecimal totalGastos = BigDecimal.ZERO;
 
    // ── Desglose por forma de pago (desde recibos_caja) ──────────
    @Column(name = "total_efectivo", precision = 10, scale = 2)
    private BigDecimal totalEfectivo = BigDecimal.ZERO;
 
    @Column(name = "total_tarjeta", precision = 10, scale = 2)
    private BigDecimal totalTarjeta = BigDecimal.ZERO;
 
    @Column(name = "total_transferencia", precision = 10, scale = 2)
    private BigDecimal totalTransferencia = BigDecimal.ZERO;
 
    @Column(name = "total_cheque", precision = 10, scale = 2)
    private BigDecimal totalCheque = BigDecimal.ZERO;
 
    @Column(name = "saldo_esperado", precision = 10, scale = 2)
    private BigDecimal saldoEsperado = BigDecimal.ZERO;
 
    @Column(name = "diferencia", precision = 10, scale = 2)
    private BigDecimal diferencia = BigDecimal.ZERO;
 
    @Column(name = "observacion", columnDefinition = "TEXT")
    private String observacion;
 
    @Column(name = "cerrado", nullable = false)
    private Boolean cerrado = false;

    @Column(name = "fecha_cierre")
    private LocalDate fechaCierre;
}