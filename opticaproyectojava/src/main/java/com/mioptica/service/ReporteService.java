package com.mioptica.service;

import com.mioptica.dto.ReporteFilaDTO;
import com.mioptica.model.ReciboCaja;
import com.mioptica.model.Venta;
import com.mioptica.repository.ReporteRepository;
import com.mioptica.repository.ReciboCajaRepository;
import com.mioptica.repository.VentaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;

@Service
@RequiredArgsConstructor
public class ReporteService {

    private final ReporteRepository    reporteRepo;
    private final VentaRepository      ventaRepo;
    private final ReciboCajaRepository reciboRepo;

    // ─── Helpers para convertir Object[] → DTO ───────────────────
    private List<ReporteFilaDTO> toDTO(List<Object[]> rows, BigDecimal totalGeneral) {
        List<ReporteFilaDTO> lista = new ArrayList<>();
        for (Object[] row : rows) {
            String     etiqueta = row[0] != null ? row[0].toString() : "—";
            Long       cantidad = row[1] instanceof Number ? ((Number) row[1]).longValue() : 0L;
            BigDecimal total    = row[2] instanceof BigDecimal ? (BigDecimal) row[2]
                                : row[2] != null ? new BigDecimal(row[2].toString())
                                : BigDecimal.ZERO;
            BigDecimal pct      = totalGeneral.compareTo(BigDecimal.ZERO) == 0
                                ? BigDecimal.ZERO
                                : total.multiply(BigDecimal.valueOf(100))
                                       .divide(totalGeneral, 1, RoundingMode.HALF_UP);
            lista.add(new ReporteFilaDTO(etiqueta, cantidad, total, pct));
        }
        return lista;
    }

    // ─── Reporte de ventas ────────────────────────────────────────
    public Map<String, Object> generarReporte(LocalDate fi, LocalDate ff,
                                               Integer idSucursal, boolean esAdmin) {
        // Admin con idSucursal=0 ve todo; otros solo su sucursal
        int idSuc = esAdmin ? 0 : idSucursal;

        BigDecimal totalGeneral = reporteRepo.totalPeriodo(fi, ff, idSuc);
        if (totalGeneral == null) totalGeneral = BigDecimal.ZERO;
        Long cantFacturas = reporteRepo.countPeriodo(fi, ff, idSuc);
        if (cantFacturas == null) cantFacturas = 0L;

        BigDecimal promedio = cantFacturas > 0
                ? totalGeneral.divide(BigDecimal.valueOf(cantFacturas), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        List<ReporteFilaDTO> porProducto  = toDTO(reporteRepo.topProductos(fi, ff, idSuc),      totalGeneral);
        List<ReporteFilaDTO> porVendedor  = toDTO(reporteRepo.ventasPorVendedor(fi, ff, idSuc), totalGeneral);
        List<ReporteFilaDTO> porCategoria = toDTO(reporteRepo.ventasPorCategoria(fi, ff, idSuc),totalGeneral);

        // Ventas por día para la gráfica
        List<Object[]> rawDia = reporteRepo.ventasPorDia(fi, ff, idSuc);
        List<String>       diasLabels = new ArrayList<>();
        List<BigDecimal>   diasTotales = new ArrayList<>();
        for (Object[] row : rawDia) {
            diasLabels.add(row[0].toString());
            diasTotales.add(row[2] instanceof BigDecimal
                    ? (BigDecimal) row[2]
                    : new BigDecimal(row[2].toString()));
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("totalGeneral",  totalGeneral);
        result.put("cantFacturas",  cantFacturas);
        result.put("promedio",      promedio);
        result.put("porProducto",   porProducto);
        result.put("porVendedor",   porVendedor);
        result.put("porCategoria",  porCategoria);
        result.put("diasLabels",    diasLabels);
        result.put("diasTotales",   diasTotales);
        return result;
    }

    // ─── Corte de Caja ────────────────────────────────────────────
    public Map<String, Object> corteDeCaja(LocalDate fecha, Integer idSucursal) {

        // Ventas del día
        List<Venta> ventas = ventaRepo.findByPeriodoYSucursal(fecha, fecha, idSucursal);
        ventas = ventas.stream().filter(v -> !"Anulada".equals(v.getEstado())).toList();

        BigDecimal totalVentas = ventas.stream()
                .map(Venta::getTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalDescuentos = ventas.stream()
                .map(Venta::getDescuento)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long ventasAnuladas = ventaRepo.findByPeriodoYSucursal(fecha, fecha, idSucursal)
                .stream().filter(v -> "Anulada".equals(v.getEstado())).count();

        // Recibos del día (incluye pagos de fichas)
        List<ReciboCaja> recibos = reciboRepo.findAll().stream()
                .filter(r -> fecha.equals(r.getFecha())
                          && r.getSucursal().getIdSucursal().equals(idSucursal))
                .toList();

        BigDecimal totalRecibos = recibos.stream()
                .map(ReciboCaja::getMonto)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Desglose por forma de pago
        Map<String, BigDecimal> porFormaPago = new LinkedHashMap<>();
        recibos.forEach(r -> {
            String forma = r.getFormaPago() != null ? r.getFormaPago() : "Contado";
            porFormaPago.merge(forma, r.getMonto(), BigDecimal::add);
        });

        Map<String, Object> corte = new LinkedHashMap<>();
        corte.put("fecha",           fecha);
        corte.put("ventas",          ventas);
        corte.put("totalVentas",     totalVentas);
        corte.put("totalDescuentos", totalDescuentos);
        corte.put("ventasAnuladas",  ventasAnuladas);
        corte.put("cantVentas",      (long) ventas.size());
        corte.put("recibos",         recibos);
        corte.put("totalRecibos",    totalRecibos);
        corte.put("porFormaPago",    porFormaPago);
        corte.put("totalEfectivo",   porFormaPago.getOrDefault("Contado", BigDecimal.ZERO));
        return corte;
    }
}
