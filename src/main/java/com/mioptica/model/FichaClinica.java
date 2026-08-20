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

    @Column(name = "fecha_siguiente_consulta")
    private LocalDate fechaSiguienteConsulta;

    @Column(name = "motivo_consulta", columnDefinition = "TEXT")
    private String motivoConsulta;

    // ─── LENSOMETRÍA (graduación lentes actuales del paciente) ────
    @Column(name = "od_esfera",   precision = 5, scale = 2) private BigDecimal odEsfera;
    @Column(name = "od_cilindro", precision = 5, scale = 2) private BigDecimal odCilindro;
    @Column(name = "od_eje")                                private Integer    odEje;
    @Column(name = "od_adicion",  precision = 5, scale = 2) private BigDecimal odAdicion;

    @Column(name = "oi_esfera",   precision = 5, scale = 2) private BigDecimal oiEsfera;
    @Column(name = "oi_cilindro", precision = 5, scale = 2) private BigDecimal oiCilindro;
    @Column(name = "oi_eje")                                private Integer    oiEje;
    @Column(name = "oi_adicion",  precision = 5, scale = 2) private BigDecimal oiAdicion;

    // ─── AGUDEZA VISUAL ───────────────────────────────────────────
    @Column(name = "av_od_sc", length = 20) private String avOdSc;
    @Column(name = "av_oi_sc", length = 20) private String avOiSc;
    @Column(name = "av_od_cc", length = 20) private String avOdCc;
    @Column(name = "av_oi_cc", length = 20) private String avOiCc;

    // AV con agujero estenopeico (permite diferenciar defecto refractivo de otra causa)
    @Column(name = "av_od_agujero", length = 20) private String avOdAgujero;
    @Column(name = "av_oi_agujero", length = 20) private String avOiAgujero;

    // AV de cerca
    @Column(name = "av_od_cerca", length = 20) private String avOdCerca;
    @Column(name = "av_oi_cerca", length = 20) private String avOiCerca;

    // ─── RX OBJETIVO (retinoscopía / autorrefracción) ──────────────
    @Column(name = "obj_od_esfera",   length = 10) private String objOdEsfera;
    @Column(name = "obj_od_cilindro", length = 10) private String objOdCilindro;
    @Column(name = "obj_od_eje",      length = 10) private String objOdEje;
    @Column(name = "obj_od_adicion",  length = 10) private String objOdAdicion;

    @Column(name = "obj_oi_esfera",   length = 10) private String objOiEsfera;
    @Column(name = "obj_oi_cilindro", length = 10) private String objOiCilindro;
    @Column(name = "obj_oi_eje",      length = 10) private String objOiEje;
    @Column(name = "obj_oi_adicion",  length = 10) private String objOiAdicion;

    // ─── RX SUBJETIVO ─────────────────────────────────────────────
    @Column(name = "sub_od_esfera",   length = 10) private String subOdEsfera;
    @Column(name = "sub_od_cilindro", length = 10) private String subOdCilindro;
    @Column(name = "sub_od_eje",      length = 10) private String subOdEje;
    @Column(name = "sub_od_adicion",  length = 10) private String subOdAdicion;

    @Column(name = "sub_oi_esfera",   length = 10) private String subOiEsfera;
    @Column(name = "sub_oi_cilindro", length = 10) private String subOiCilindro;
    @Column(name = "sub_oi_eje",      length = 10) private String subOiEje;
    @Column(name = "sub_oi_adicion",  length = 10) private String subOiAdicion;

    // ─── RX FINAL ─────────────────────────────────────────────────
    @Column(name = "rx_od_esfera",   length = 10) private String rxOdEsfera;
    @Column(name = "rx_od_cilindro", length = 10) private String rxOdCilindro;
    @Column(name = "rx_od_eje",      length = 10) private String rxOdEje;
    @Column(name = "rx_od_adicion",  length = 10) private String rxOdAdicion;
    @Column(name = "rx_od_altura",   precision = 5, scale = 2) private BigDecimal rxOdAltura;

    @Column(name = "rx_oi_esfera",   length = 10) private String rxOiEsfera;
    @Column(name = "rx_oi_cilindro", length = 10) private String rxOiCilindro;
    @Column(name = "rx_oi_eje",      length = 10) private String rxOiEje;
    @Column(name = "rx_oi_adicion",  length = 10) private String rxOiAdicion;
    @Column(name = "rx_oi_altura",   precision = 5, scale = 2) private BigDecimal rxOiAltura;

    @Column(name = "rx_dip",    precision = 5, scale = 2) private BigDecimal rxDip;
    @Column(name = "rx_ndp_od", precision = 5, scale = 2) private BigDecimal rxNdpOd;
    @Column(name = "rx_ndp_oi", precision = 5, scale = 2) private BigDecimal rxNdpOi;

    @Column(name = "rx_segmento", length = 100) private String rxSegmento;

    // Medidas de ajuste del armazón (se envían a la Orden de Laboratorio)
    @Column(name = "vertex",       length = 10) private String vertex;        // mm
    @Column(name = "pantoscopico", length = 10) private String pantoscopico;  // °
    @Column(name = "panoramico",   length = 10) private String panoramico;    // °

    // ─── SUGERENCIA DE MATERIALES ─────────────────────────────────
    @Column(name = "sug_tipo_lente",     length = 50)  private String sugTipoLente;
    @Column(name = "sug_material_lente", length = 50)  private String sugMaterialLente;
    @Column(name = "sug_color",          length = 50)  private String sugColor;
    @Column(name = "sug_observaciones",  columnDefinition = "TEXT") private String sugObservaciones;
    @Column(name = "sug_tratamientos",   columnDefinition = "TEXT") private String sugTratamientos;

    // ─── PEDIDO ───────────────────────────────────────────────────
    @Column(name = "armazon",            length = 200) private String     armazon;
    @Column(name = "detalle_lentes",     columnDefinition = "TEXT") private String detalleLentes;

    @Column(name = "total",  precision = 10, scale = 2) private BigDecimal total  = BigDecimal.ZERO;
    @Column(name = "abono",  precision = 10, scale = 2) private BigDecimal abono  = BigDecimal.ZERO;
    @Column(name = "saldo",  precision = 10, scale = 2) private BigDecimal saldo  = BigDecimal.ZERO;

    @Column(name = "fecha_entrega")      private LocalDate fechaEntrega;
    @Column(name = "fecha_entrega_real") private LocalDate fechaEntregaReal;

    @Column(name = "estado_entrega", length = 30)
    private String estadoEntrega = "Pendiente";

    @Column(name = "observaciones", columnDefinition = "TEXT")
    private String observaciones;

    @Column(name = "historia_clinica", columnDefinition = "TEXT")
    private String historiaClinica;

    // ─── Helpers calculados ───────────────────────────────────────
    public long getDiasPendiente() {
        if (fecha == null) return 0;
        return ChronoUnit.DAYS.between(fecha, LocalDate.now());
    }

    public long getDiasParaEntrega() {
        if (fechaEntrega == null) return 999;
        return ChronoUnit.DAYS.between(LocalDate.now(), fechaEntrega);
    }

    public int getPorcentajePagado() {
        if (total == null || total.compareTo(BigDecimal.ZERO) == 0) return 0;
        BigDecimal pagado = total.subtract(saldo != null ? saldo : BigDecimal.ZERO);
        return pagado.multiply(BigDecimal.valueOf(100))
                     .divide(total, 0, java.math.RoundingMode.HALF_UP)
                     .intValue();
    }

    public String getSemaforoEntrega() {
        long dias = getDiasParaEntrega();
        if (dias < 0)    return "rojo";
        if (dias == 0)   return "naranja";
        if (dias <= 3)   return "amarillo";
        if (dias <= 7)   return "azul";
        return "verde";
    }

    /** Fecha en la que el paciente cumple 1 año desde esta consulta. */
    public LocalDate getFechaAniversario() {
        if (fecha == null) return null;
        return fecha.plusYears(1);
    }

    /** Días que faltan para que el paciente cumpla el año desde esta consulta (negativo = ya lo cumplió). */
    public long getDiasParaAniversario() {
        LocalDate aniversario = getFechaAniversario();
        if (aniversario == null) return 9999;
        return ChronoUnit.DAYS.between(LocalDate.now(), aniversario);
    }

    /** true si al paciente le faltan 30 días o menos para cumplir el año de su última consulta (o ya lo cumplió). */
    public boolean isProximaACumplirAnio() {
        long dias = getDiasParaAniversario();
        return dias <= 30;
    }
}