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
    // ── Receta externa (opcional) ──────────────────────────────────
    private boolean recetaExterna = false;

    // Graduación externa OD
    private String rxOdEsfera;
    private String rxOdCilindro;
    private String rxOdEje;
    private String rxOdAdd;

    // Graduación externa OI
    private String rxOiEsfera;
    private String rxOiCilindro;
    private String rxOiEje;
    private String rxOiAdd;

    // Medidas
    private String rxPantoscopico;
    private String rxVertex;
    private String rxPanoramico;

    private Integer rxIdCliente; // cliente para la orden (puede ser null=CF)

    @Data
    public static class ItemVenta {
        private Integer    idProducto;
        private BigDecimal cantidad;
        private BigDecimal precioUnitario;
        private BigDecimal descuento;
        private BigDecimal subtotal;
    }
}
