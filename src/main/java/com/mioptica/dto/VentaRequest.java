package com.mioptica.dto;

import java.math.BigDecimal;
import java.util.List;
import lombok.Data;

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

    // ── TAREA 1: Campos nuevos ─────────────────────────────────────
    private String rxDpOd;        // Distancia pupilar OD
    private String rxDpOi;        // Distancia pupilar OI
    private String rxAlturaOd;    // Altura OD
    private String rxAlturaOi;    // Altura OI
    private String rxSegmento;    // Segmento
   private String rxLenteRecomendado; // Lente recomendado (tipo/material)

    // TAREA 2: Fecha de entrega
    private String fechaEntrega;       // formato "yyyy-MM-dd" viene del input date

    // TAREA 3: Lugar y sucursal de entrega
    private String  lugarEntrega;
    private Integer idSucursalEntrega;

// TAREA 4: Datos de transferencia
    private String referenciaPago;   // número de referencia
    private String bancoPago;        // banco emisor

    // TAREA 7: Fecha estimada de entrega de la orden de laboratorio
    private String fechaEntregaOrden; // formato "yyyy-MM-dd"

    // ── Ficha Clínica Interna (cargada automáticamente desde DB) ──
    private boolean fichaInterna = false;

    // Graduación Rx Final OD
    private String fiOdEsfera;
    private String fiOdCilindro;
    private String fiOdEje;
    private String fiOdAdd;

    // Graduación Rx Final OI
    private String fiOiEsfera;
    private String fiOiCilindro;
    private String fiOiEje;
    private String fiOiAdd;

    // Medidas del paciente (ficha interna)
    private String fiDpOd;
    private String fiDpOi;
    private String fiAlturaOd;
    private String fiAlturaOi;
    private String fiSegmento;
    private String fiLenteRecomendado;
    private String fiFechaEntregaOrden;

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