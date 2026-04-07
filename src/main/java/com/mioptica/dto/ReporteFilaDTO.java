package com.mioptica.dto;

import lombok.Data;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;

/** Fila de un reporte agrupado (por producto, por vendedor, por categoría, etc.) */
@Data
@AllArgsConstructor
public class ReporteFilaDTO {
    private String     etiqueta;   // nombre del grupo
    private Long       cantidad;   // unidades vendidas
    private BigDecimal total;      // monto total
    private BigDecimal porcentaje; // del total general
}
