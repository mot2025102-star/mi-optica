package com.mioptica.service;

import com.mioptica.dto.ReporteFilaDTO;
import com.mioptica.dto.VentaDetalleDTO;
import com.mioptica.model.CorteCaja;
import com.mioptica.model.GastoCaja;
import com.mioptica.model.ReciboCaja;
import com.mioptica.model.Venta;
import com.mioptica.repository.ReporteRepository;
import com.mioptica.repository.ReciboCajaRepository;
import com.mioptica.repository.VentaRepository;
import com.mioptica.repository.CorteCajaRepository;
import com.mioptica.repository.GastoCajaRepository;
import lombok.RequiredArgsConstructor;
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
    private final CorteCajaRepository  corteCajaRepo;  // ── NUEVO
    private final GastoCajaRepository  gastoCajaRepo;  // ── NUEVO

    // ─── Helper: Object[] → ReporteFilaDTO ───────────────────────
    private List<ReporteFilaDTO> toDTO(List<Object[]> rows, BigDecimal totalGeneral) {
        List<ReporteFilaDTO> lista = new ArrayList<>();
        for (Object[] row : rows) {
            String     etiqueta = row[0] != null ? row[0].toString() : "—";
            Long       cantidad = row[1] instanceof Number ? ((Number) row[1]).longValue() : 0L;
            BigDecimal total    = toBD(row[2]);
            BigDecimal pct      = totalGeneral.compareTo(BigDecimal.ZERO) == 0
                                ? BigDecimal.ZERO
                                : total.multiply(BigDecimal.valueOf(100))
                                       .divide(totalGeneral, 1, RoundingMode.HALF_UP);
            lista.add(new ReporteFilaDTO(etiqueta, cantidad, total, pct));
        }
        return lista;
    }

    // ─── Helper: convierte Object a BigDecimal seguro ─────────────
    private BigDecimal toBD(Object val) {
        if (val instanceof BigDecimal) return (BigDecimal) val;
        if (val != null) return new BigDecimal(val.toString());
        return BigDecimal.ZERO;
    }

    // ─── Helper: Object[] → VentaDetalleDTO ──────────────────────
    private List<VentaDetalleDTO> toDetalleDTO(List<Object[]> rows) {
        List<VentaDetalleDTO> lista = new ArrayList<>();
        for (Object[] row : rows) {
            String     numeroFactura = row[0] != null ? row[0].toString() : "—";
            LocalDate  fecha         = row[1] instanceof LocalDate ? (LocalDate) row[1] : null;
            String     vendedor      = row[2] != null ? row[2].toString() : "—";
            String     categoria     = row[3] != null ? row[3].toString() : "Sin categoría";
            String     producto      = row[4] != null ? row[4].toString() : "—";
            Long       cantidad      = row[5] instanceof Number ? ((Number) row[5]).longValue() : 0L;
            BigDecimal precioVenta   = toBD(row[6]);
            String     formaPago     = row[7] != null ? row[7].toString() : "—";

            lista.add(new VentaDetalleDTO(numeroFactura, fecha, vendedor,
                    categoria, producto, cantidad, precioVenta, formaPago));
        }
        return lista;
    }

    // ─── Reporte de ventas ────────────────────────────────────────
    public Map<String, Object> generarReporte(LocalDate fi, LocalDate ff,
                                               Integer idSucursal, boolean esAdmin,
                                               Integer idCategoria, Integer idVendedor) {
        int idSuc = esAdmin ? idSucursal : idSucursal;

        BigDecimal totalGeneral = reporteRepo.totalPeriodo(fi, ff, idSuc);
        if (totalGeneral == null) totalGeneral = BigDecimal.ZERO;

        Long cantFacturas = reporteRepo.countPeriodo(fi, ff, idSuc);
        if (cantFacturas == null) cantFacturas = 0L;

        BigDecimal promedio = cantFacturas > 0
                ? totalGeneral.divide(BigDecimal.valueOf(cantFacturas), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        List<ReporteFilaDTO> porProducto  = toDTO(reporteRepo.topProductos(fi, ff, idSuc),       totalGeneral);
        List<ReporteFilaDTO> porVendedor  = toDTO(reporteRepo.ventasPorVendedor(fi, ff, idSuc),  totalGeneral);
        List<ReporteFilaDTO> porCategoria = toDTO(reporteRepo.ventasPorCategoria(fi, ff, idSuc), totalGeneral);

        List<Object[]>   rawDia     = reporteRepo.ventasPorDia(fi, ff, idSuc);
        List<String>     diasLabels = new ArrayList<>();
        List<BigDecimal> diasTotales = new ArrayList<>();
        for (Object[] row : rawDia) {
            diasLabels.add(row[0].toString());
            diasTotales.add(toBD(row[2]));
        }

        List<VentaDetalleDTO> detalleVentas = toDetalleDTO(
                reporteRepo.detalleVentas(fi, ff, idSuc, idCategoria, idVendedor));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("totalGeneral",  totalGeneral);
        result.put("cantFacturas",  cantFacturas);
        result.put("promedio",      promedio);
        result.put("porProducto",   porProducto);
        result.put("porVendedor",   porVendedor);
        result.put("porCategoria",  porCategoria);
        result.put("diasLabels",    diasLabels);
        result.put("diasTotales",   diasTotales);
        result.put("detalleVentas", detalleVentas);
        return result;
    }

    // ─── Corte de Caja ────────────────────────────────────────────
    // ── ACTUALIZADO: ahora incluye saldo inicial, gastos y diferencia
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

        // Recibos del día
        List<ReciboCaja> recibos = reciboRepo.findAll().stream()
                .filter(r -> fecha.equals(r.getFecha())
                          && r.getSucursal().getIdSucursal().equals(idSucursal))
                .toList();

        BigDecimal totalRecibos = recibos.stream()
                .map(ReciboCaja::getMonto)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<String, BigDecimal> porFormaPago = new LinkedHashMap<>();
        recibos.forEach(r -> {
            String forma = r.getFormaPago() != null ? r.getFormaPago() : "Contado";
            porFormaPago.merge(forma, r.getMonto(), BigDecimal::add);
        });

        // ── NUEVO: Gastos del día ──────────────────────────────────
        List<GastoCaja> gastos = gastoCajaRepo
                .findByFechaYSucursal(fecha, idSucursal);
        BigDecimal totalGastos = gastoCajaRepo
                .totalGastosDia(fecha, idSucursal);
        if (totalGastos == null) totalGastos = BigDecimal.ZERO;

        // ── NUEVO: Corte guardado (saldo inicial, diferencia, etc.) ─
        Optional<CorteCaja> corteGuardado = corteCajaRepo
                .findByIdSucursalAndFecha(idSucursal, fecha);

        BigDecimal saldoInicial  = corteGuardado.map(CorteCaja::getSaldoInicial)
                                                 .orElse(BigDecimal.ZERO);
        BigDecimal saldoFisico   = corteGuardado.map(CorteCaja::getSaldoFisico)
                                                 .orElse(null);
        BigDecimal saldoEsperado = saldoInicial.add(totalRecibos).subtract(totalGastos);
        BigDecimal diferencia    = saldoFisico != null
                                 ? saldoFisico.subtract(saldoEsperado)
                                 : BigDecimal.ZERO;
        boolean    cerrado       = corteGuardado.map(CorteCaja::getCerrado).orElse(false);
        Integer    idCorte       = corteGuardado.map(CorteCaja::getIdCorte).orElse(null);

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
        // ── NUEVO ──────────────────────────────────────────────────
        corte.put("gastos",          gastos);
        corte.put("totalGastos",     totalGastos);
        corte.put("saldoInicial",    saldoInicial);
        corte.put("saldoFisico",     saldoFisico);
        corte.put("saldoEsperado",   saldoEsperado);
        corte.put("diferencia",      diferencia);
        corte.put("cerrado",         cerrado);
        corte.put("idCorte",         idCorte);
        return corte;
    }
}