package com.mioptica.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Entity
@Table(name = "correlativos",
       uniqueConstraints = @UniqueConstraint(columnNames = {"id_sucursal","tipo"}))
public class Correlativo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_sucursal", nullable = false)
    private Sucursal sucursal;

    @Column(name = "tipo", nullable = false, length = 30)
    private String tipo; // Factura | Recibo | Ingreso | Traslado | Cotizacion

    @Column(name = "valor_actual", nullable = false)
    private Integer valorActual = 0;
}