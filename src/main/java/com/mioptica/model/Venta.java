package com.mioptica.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@Entity
@Table(name = "ventas")
public class Venta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_venta")
    private Integer idVenta;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_sucursal", nullable = false)
    private Sucursal sucursal;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_usuario", nullable = false)
    private Usuario usuario;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_cliente")
    private Cliente cliente;  // null = Consumidor Final

    @Column(name = "numero_factura", unique = true, length = 20)
    private String numeroFactura;

    @Column(name = "fecha", nullable = false)
    private LocalDate fecha;

    @Column(name = "subtotal", precision = 10, scale = 2)
    private BigDecimal subtotal = BigDecimal.ZERO;

    @Column(name = "descuento", precision = 10, scale = 2)
    private BigDecimal descuento = BigDecimal.ZERO;

    @Column(name = "total", precision = 10, scale = 2)
    private BigDecimal total = BigDecimal.ZERO;

    @Column(name = "estado", length = 20)
    private String estado = "Pagada";

    @Column(name = "estado_pago", length = 20)
    private String estadoPago = "COMPLETO";

    @Column(name = "saldo_pendiente", precision = 10, scale = 2)
    private BigDecimal saldoPendiente = BigDecimal.ZERO;

    @Column(name = "observacion", columnDefinition = "TEXT")
    private String observacion;

    // TAREA 2: Fecha de entrega del pedido
    @Column(name = "fecha_entrega")
    private LocalDate fechaEntrega;

    // TAREA 3: Lugar y sucursal de entrega
    @Column(name = "lugar_entrega", length = 200)
    private String lugarEntrega;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_sucursal_entrega")
    private Sucursal sucursalEntrega;

    // Estado de entrega física del pedido al cliente
    @Column(name = "estado_entrega", length = 30)
    private String estadoEntrega = "Pendiente";

    @Column(name = "fecha_entrega_real")
    private LocalDate fechaEntregaReal;

    // Relación con el detalle (cascada: al guardar la venta, guarda el detalle)
    @OneToMany(mappedBy = "venta", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DetalleVenta> detalles = new ArrayList<>();

    // Relación con los pagos
    @OneToMany(mappedBy = "venta", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PagoVenta> pagos = new ArrayList<>();
    // ─── Helpers calculados (usados en la plantilla de Saldos y Entregas) ────

    /** Días que faltan para la fecha de entrega (negativo = ya venció). */
    public long getDiasParaEntrega() {
        if (fechaEntrega == null) return 999;
        return ChronoUnit.DAYS.between(LocalDate.now(), fechaEntrega);
    }

    /** Color del semáforo según días para la entrega. */
    public String getSemaforoEntrega() {
        long dias = getDiasParaEntrega();
        if (dias < 0)    return "rojo";
        if (dias == 0)   return "naranja";
        if (dias <= 3)   return "amarillo";
        if (dias <= 7)   return "azul";
        return "verde";
    }

    /** Días transcurridos desde la fecha de la venta hasta hoy. */
    public long getDiasPendiente() {
        if (fecha == null) return 0;
        return ChronoUnit.DAYS.between(fecha, LocalDate.now());
    }

    /** Porcentaje ya pagado sobre el total de la venta. */
    public int getPorcentajePagado() {
        if (total == null || total.compareTo(BigDecimal.ZERO) == 0) return 0;
        BigDecimal pagado = total.subtract(saldoPendiente != null ? saldoPendiente : BigDecimal.ZERO);
        return pagado.multiply(BigDecimal.valueOf(100))
                     .divide(total, 0, RoundingMode.HALF_UP)
                     .intValue();
    }
}