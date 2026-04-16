package com.mioptica.dto;

import lombok.Data;
import java.time.LocalDate;
import java.util.List;

@Data
public class OrdenLabRequest {

    // Cliente
    private Integer idCliente;   // null = CF

    // Clínico OD
    private String odEsfera, odCilindro, odEje, odAdd, odDip, odNdpod, odNdpoi, odAltura;

    // Clínico OI
    private String oiEsfera, oiCilindro, oiEje, oiAdd, oiDip, oiNdpod, oiNdpoi, oiAltura;

    // Medidas
    private String pantoscopico, vertex, panoramico;

    // Proceso
    private LocalDate fechaEnvioLab;
    private LocalDate fechaEntregaEstimada;
    private String observaciones;

    // Productos
    private List<DetalleOrdenLabDto> productos;

    @Data
    public static class DetalleOrdenLabDto {
        private String  codigo;
        private Integer cantidad;
        private String  descripcion;
        private String  material;
        private String  tratamiento;
        private String  colorTinte;
    }
}
