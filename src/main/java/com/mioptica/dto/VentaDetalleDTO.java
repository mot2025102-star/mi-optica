package com.mioptica.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@AllArgsConstructor
public class VentaDetalleDTO {
    private String     numeroFactura;
    private LocalDate  fecha;
    private String     vendedor;
    private String     categoria;
    private String     producto;
    private Long       cantidad;
    private BigDecimal precioVenta;
    private String     formaPago;
}
