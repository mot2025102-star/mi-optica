package com.mioptica.dto;

import lombok.Data;
import lombok.AllArgsConstructor;
import java.math.BigDecimal;

@Data
@AllArgsConstructor
public class ReporteFilaDTO {
    private String     etiqueta;
    private Long       cantidad;
    private BigDecimal total;
    private BigDecimal porcentaje;
}
