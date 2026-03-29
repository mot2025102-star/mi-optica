package com.mioptica.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

/**
 * Objeto que recibe el JSON del formulario de nueva venta.
 * El frontend envía esto vía fetch() al endpoint POST /ventas/registrar
 */
@Data
public class VentaRequest {

    private Integer   idCliente;       // null = Consumidor Final
    private String    formaPago;       // Contado | Tarjeta | Transferencia | Cheque
    private BigDecimal descuentoGlobal;
    private String    observacion;
    private List<ItemVenta> items;

    @Data
    public static class ItemVenta {
        private Integer    idProducto;
        private BigDecimal cantidad;
        private BigDecimal precioUnitario;
        private BigDecimal descuento;
        private BigDecimal subtotal;
    }
}
