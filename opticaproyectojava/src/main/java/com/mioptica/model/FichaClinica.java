package com.mioptica.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

@Data
@NoArgsConstructor
@Entity
@Table(name = "fichas_clinicas")
public class FichaClinica {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_ficha")
    private Integer idFicha;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_cliente", nullable = false)
    private Cliente cliente;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_sucursal", nullable = false)
    private Sucursal sucursal;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_optometrista", nullable = false)
    private Usuario optometrista;

    @Column(name = "fecha", nullable = false)
    private LocalDate fecha;

    @Column(name = "motivo_consulta", columnDefinition = "TEXT")
    private String motivoConsulta;

    // ─── Graduación OD ────────────────────────────────────────────
    @Column(name = "od_esfera",   precision = 5, scale = 2) private BigDecimal odEsfera;
    @Column(name = "od_cilindro", precision = 5, scale = 2) private BigDecimal odCilindro;
    @Column(name = "od_eje")                                private Integer    odEje;
    @Column(name = "od_adicion",  precision = 5, scale = 2) private BigDecimal odAdicion;

    // ─── Graduación OI ────────────────────────────────────────────
    @Column(name = "oi_esfera",   precision = 5, scale = 2) private BigDecimal oiEsfera;
    @Column(name = "oi_cilindro", precision = 5, scale = 2) private BigDecimal oiCilindro;
    @Column(name = "oi_eje")                                private Integer    oiEje;
    @Column(name = "oi_adicion",  precision = 5, scale = 2) private BigDecimal oiAdicion;

    // ─── Agudeza visual ───────────────────────────────────────────
    @Column(name = "av_od_sc", length = 20) private String avOdSc;
    @Column(name = "av_oi_sc", length = 20) private String avOiSc;
    @Column(name = "av_od_cc", length = 20) private String avOdCc;
    @Column(name = "av_oi_cc", length = 20) private String avOiCc;

    // ─── Pedido ───────────────────────────────────────────────────
    @Column(name = "armazon",       length = 200) private String     armazon;
    @Column(name = "detalle_lentes", columnDefinition = "TEXT") private String detalleLentes;

    @Column(name = "total",  precision = 10, scale = 2) private BigDecimal total  = BigDecimal.ZERO;
    @Column(name = "abono",  precision = 10, scale = 2) private BigDecimal abono  = BigDecimal.ZERO;
    @Column(name = "saldo",  precision = 10, scale = 2) private BigDecimal saldo  = BigDecimal.ZERO;

    @Column(name = "fecha_entrega")      private LocalDate fechaEntrega;
    @Column(name = "fecha_entrega_real") private LocalDate fechaEntregaReal;

    @Column(name = "estado_entrega", length = 30)
    private String estadoEntrega = "Pendiente";

    @Column(name = "observaciones", columnDefinition = "TEXT")
    private String observaciones;

    // ─── Helpers calculados ───────────────────────────────────────

    /** Días que lleva pendiente el saldo desde la fecha de la ficha */
    public long getDiasPendiente() {
        if (fecha == null) return 0;
        return ChronoUnit.DAYS.between(fecha, LocalDate.now());
    }

    /** Días para la entrega (negativo = vencido) */
    public long getDiasParaEntrega() {
        if (fechaEntrega == null) return 999;
        return ChronoUnit.DAYS.between(LocalDate.now(), fechaEntrega);
    }

    /** Porcentaje ya pagado */
    public int getPorcentajePagado() {
        if (total == null || total.compareTo(BigDecimal.ZERO) == 0) return 0;
        BigDecimal pagado = total.subtract(saldo != null ? saldo : BigDecimal.ZERO);
        return pagado.multiply(BigDecimal.valueOf(100))
                     .divide(total, 0, java.math.RoundingMode.HALF_UP)
                     .intValue();
    }

    /** Color semáforo para la entrega */
    public String getSemaforoEntrega() {
        long dias = getDiasParaEntrega();
        if (dias < 0)    return "rojo";     // vencido
        if (dias == 0)   return "naranja";  // hoy
        if (dias <= 3)   return "amarillo"; // en 3 días
        if (dias <= 7)   return "azul";     // esta semana
        return "verde";                     // más adelante
    }
}
