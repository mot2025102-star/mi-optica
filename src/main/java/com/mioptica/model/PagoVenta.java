package com.mioptica.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@Entity
@Table(name = "pagos_venta")
public class PagoVenta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_pago")
    private Integer idPago;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_venta", nullable = false)
    private Venta venta;

    @Column(name = "metodo_pago", nullable = false, length = 50)
    private String metodoPago;

    @Column(name = "monto", nullable = false, precision = 10, scale = 2)
    private BigDecimal monto;

    // Campos opcionales para Tarjeta
    @Column(name = "tipo_tarjeta", length = 50)
    private String tipoTarjeta;

    @Column(name = "marca_tarjeta", length = 50)
    private String marcaTarjeta;

    @Column(name = "ultimos_digitos", length = 4)
    private String ultimosDigitos;

    @Column(name = "autorizacion", length = 100)
    private String autorizacion;

    // Campos opcionales para Cheque
    @Column(name = "banco", length = 100)
    private String banco;

    @Column(name = "no_cheque", length = 50)
    private String noCheque;

    @Column(name = "titular", length = 100)
    private String titular;

    @Column(name = "fecha_cheque", length = 20)
    private String fechaCheque;
}
