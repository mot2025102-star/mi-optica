package com.mioptica.controller;

import com.mioptica.dto.ReporteFilaDTO;
import com.mioptica.repository.ReporteRepository;
import com.mioptica.repository.UsuarioRepository;
import com.mioptica.service.ReporteService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;

@RestController
@RequestMapping("/reportes/analiticas")
@RequiredArgsConstructor
public class ReporteAnaliticasController {

    private final ReporteRepository reporteRepo;
    private final UsuarioRepository usuarioRepo;
    private final ReporteService reporteService;

    // ── Helpers ────────────────────────────────────────────────────
    private Integer resolveIdSuc(UserDetails ud, Integer paramIdSuc) {
        var usuario = usuarioRepo.findByUsername(ud.getUsername()).orElseThrow();
        if (!usuario.esAdmin() && usuario.getSucursal() != null) {
            return usuario.getSucursal().getIdSucursal();
        }
        return paramIdSuc != null ? paramIdSuc : 0;
    }

    // ── 1. Tendencia de ventas ──────────────────────────────────────────
    @GetMapping("/tendencia")
    public ResponseEntity<Map<String, Object>> tendencia(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fi,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate ff,
            @RequestParam(defaultValue = "0") Integer idSucursal,
            @AuthenticationPrincipal UserDetails ud) {

        Integer idSuc = resolveIdSuc(ud, idSucursal);
        
        long dias = ChronoUnit.DAYS.between(fi, ff);
        List<Object[]> raw;
        
        if (dias <= 31) {
            raw = reporteRepo.ventasPorDia(fi, ff, idSuc);
        } else {
            raw = reporteRepo.ventasPorMes(fi, ff, idSuc);
        }

        List<String> labels = new ArrayList<>();
        List<Double> data = new ArrayList<>();
        String[] meses = {"Ene", "Feb", "Mar", "Abr", "May", "Jun", "Jul", "Ago", "Sep", "Oct", "Nov", "Dic"};

        for (Object[] r : raw) {
            String lbl = r[0].toString();
            // Si es mes (YYYY-MM), lo formateamos
            if (dias > 31 && lbl.length() == 7) {
                try {
                    String[] parts = lbl.split("-");
                    int m = Integer.parseInt(parts[1]);
                    lbl = meses[m - 1] + " " + parts[0];
                } catch (Exception ignored) {}
            }
            labels.add(lbl); 
            data.add(((Number) r[2]).doubleValue());
        }

        return ResponseEntity.ok(Map.of("labels", labels, "data", data));
    }

    // ── 2. Ventas por día de semana (Modificado para recibir mes/año) ──────────
    @GetMapping("/por-dia-semana")
    public ResponseEntity<Map<String, Object>> porDiaSemana(
            @RequestParam Integer mes,
            @RequestParam Integer anio,
            @RequestParam(defaultValue = "0") Integer idSucursal,
            @AuthenticationPrincipal UserDetails ud) {

        Integer idSuc = resolveIdSuc(ud, idSucursal);
        
        LocalDate fi = LocalDate.of(anio, mes, 1);
        LocalDate ff = fi.withDayOfMonth(fi.lengthOfMonth());
        
        List<Object[]> raw = reporteRepo.ventasPorDiaSemana(fi, ff, idSuc);

        String[] nombresDias = {"Dom", "Lun", "Mar", "Mié", "Jue", "Vie", "Sáb"};
        double[] totales = new double[7]; 

        for (Object[] r : raw) {
            int dow = ((Number) r[0]).intValue();
            double total = ((Number) r[1]).doubleValue();
            if (dow >= 1 && dow <= 7) {
                totales[dow - 1] = total;
            }
        }

        List<String> labels = Arrays.asList("Lun", "Mar", "Mié", "Jue", "Vie", "Sáb", "Dom");
        List<Double> data = Arrays.asList(
                totales[1], totales[2], totales[3], totales[4], totales[5], totales[6], totales[0]
        );

        return ResponseEntity.ok(Map.of("labels", labels, "data", data));
    }

    // ── 3. Ventas por mes ──────────────────────────────────────────
    @GetMapping("/por-mes")
    public ResponseEntity<Map<String, Object>> porMes(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fi,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate ff,
            @RequestParam(defaultValue = "0") Integer idSucursal,
            @AuthenticationPrincipal UserDetails ud) {

        Integer idSuc = resolveIdSuc(ud, idSucursal);
        List<Object[]> raw = reporteRepo.ventasPorMes(fi, ff, idSuc);

        List<String> labels = new ArrayList<>();
        List<Double> data = new ArrayList<>();

        String[] meses = {"Ene", "Feb", "Mar", "Abr", "May", "Jun", "Jul", "Ago", "Sep", "Oct", "Nov", "Dic"};

        for (Object[] r : raw) {
            String mesAno = r[0].toString(); // "2024-05"
            try {
                String[] parts = mesAno.split("-");
                int m = Integer.parseInt(parts[1]);
                String label = meses[m - 1] + " " + parts[0];
                labels.add(label);
                data.add(((Number) r[2]).doubleValue());
            } catch (Exception e) {
                labels.add(mesAno);
                data.add(((Number) r[2]).doubleValue());
            }
        }

        return ResponseEntity.ok(Map.of("labels", labels, "data", data));
    }

    // ── 4. Top productos por cantidad ──────────────────────────────
    @GetMapping("/top-productos")
    public ResponseEntity<Map<String, Object>> topProductos(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fi,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate ff,
            @RequestParam(defaultValue = "0") Integer idSucursal,
            @AuthenticationPrincipal UserDetails ud) {

        Integer idSuc = resolveIdSuc(ud, idSucursal);
        List<Object[]> raw = reporteRepo.topProductosPorCantidad(fi, ff, idSuc);

        List<String> labels = new ArrayList<>();
        List<Long> data = new ArrayList<>();

        for (Object[] r : raw) {
            String prod = r[0] != null ? r[0].toString() : "Desconocido";
            if (prod.length() > 25) prod = prod.substring(0, 22) + "...";
            labels.add(prod);
            data.add(((Number) r[1]).longValue());
        }

        return ResponseEntity.ok(Map.of("labels", labels, "data", data));
    }

    // ── 5. Ventas por categoría (NUEVA) ────────────────────────────
    @GetMapping("/por-categoria")
    public ResponseEntity<Map<String, Object>> porCategoria(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fi,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate ff,
            @RequestParam(defaultValue = "0") Integer idSucursal,
            @AuthenticationPrincipal UserDetails ud) {

        boolean esAdmin = usuarioRepo.findByUsername(ud.getUsername()).orElseThrow().esAdmin();
        Map<String, Object> reporte = reporteService.generarReporte(fi, ff, idSucursal, esAdmin, 0, 0);
        
        @SuppressWarnings("unchecked")
        List<ReporteFilaDTO> porCategoria = (List<ReporteFilaDTO>) reporte.get("porCategoria");
        
        List<String> labels = new ArrayList<>();
        List<Double> data = new ArrayList<>();
        
        for (ReporteFilaDTO fila : porCategoria) {
            labels.add(fila.getEtiqueta());
            data.add(fila.getTotal().doubleValue());
        }

        return ResponseEntity.ok(Map.of("labels", labels, "data", data));
    }

    // ── 6. Margen de ganancia por producto (NUEVA) ─────────────────
    @GetMapping("/margen-producto")
    public ResponseEntity<Map<String, Object>> margenProducto(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fi,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate ff,
            @RequestParam(defaultValue = "0") Integer idSucursal,
            @AuthenticationPrincipal UserDetails ud) {

        boolean esAdmin = usuarioRepo.findByUsername(ud.getUsername()).orElseThrow().esAdmin();
        Map<String, Object> reporte = reporteService.generarReporte(fi, ff, idSucursal, esAdmin, 0, 0);
        
        @SuppressWarnings("unchecked")
        List<ReporteFilaDTO> porProducto = (List<ReporteFilaDTO>) reporte.get("porProducto");
        
        // Ordenar por margen descendente
        porProducto.sort((a, b) -> b.getMargen().compareTo(a.getMargen()));
        
        List<String> labels = new ArrayList<>();
        List<Double> data = new ArrayList<>();
        
        int count = 0;
        for (ReporteFilaDTO fila : porProducto) {
            if (count >= 10) break; // Top 10
            if (fila.getMargen().doubleValue() <= 0) continue; // Solo mostrar si hay margen
            String prod = fila.getEtiqueta();
            if (prod.length() > 25) prod = prod.substring(0, 22) + "...";
            labels.add(prod);
            data.add(fila.getMargen().doubleValue());
            count++;
        }

        return ResponseEntity.ok(Map.of("labels", labels, "data", data));
    }

    // ── 7. Ranking de vendedores (NUEVA) ───────────────────────────
    @GetMapping("/ranking-vendedores")
    public ResponseEntity<Map<String, Object>> rankingVendedores(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fi,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate ff,
            @RequestParam(defaultValue = "0") Integer idSucursal,
            @AuthenticationPrincipal UserDetails ud) {

        boolean esAdmin = usuarioRepo.findByUsername(ud.getUsername()).orElseThrow().esAdmin();
        Map<String, Object> reporte = reporteService.generarReporte(fi, ff, idSucursal, esAdmin, 0, 0);
        
        @SuppressWarnings("unchecked")
        List<ReporteFilaDTO> porVendedor = (List<ReporteFilaDTO>) reporte.get("porVendedor");
        
        // Ordenar por total descendente
        porVendedor.sort((a, b) -> b.getTotal().compareTo(a.getTotal()));
        
        List<String> labels = new ArrayList<>();
        List<Double> data = new ArrayList<>();
        
        for (ReporteFilaDTO fila : porVendedor) {
            labels.add(fila.getEtiqueta());
            data.add(fila.getTotal().doubleValue());
        }

        return ResponseEntity.ok(Map.of("labels", labels, "data", data));
    }

    // ── 8. Comparación entre sucursales ────────────────────────────
    @GetMapping("/por-sucursal")
    public ResponseEntity<List<Map<String, Object>>> porSucursal(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fi,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate ff) {

        List<Object[]> rawResumen = reporteRepo.resumenPorSucursal(fi, ff);
        List<Map<String, Object>> result = new ArrayList<>();

        for (Object[] r : rawResumen) {
            Integer idSuc = ((Number) r[0]).intValue();
            String nombre = r[1].toString();
            Long facturas = ((Number) r[2]).longValue();
            Double total = ((Number) r[3]).doubleValue();

            List<Object[]> rawDias = reporteRepo.ventasPorDiaPorSucursal(fi, ff, idSuc);
            List<String> diasLabels = new ArrayList<>();
            List<Double> diasData = new ArrayList<>();

            for (Object[] d : rawDias) {
                diasLabels.add(d[0].toString());
                diasData.add(((Number) d[1]).doubleValue());
            }

            Map<String, Object> sucMap = new HashMap<>();
            sucMap.put("idSucursal", idSuc);
            sucMap.put("nombre", nombre);
            sucMap.put("facturas", facturas);
            sucMap.put("total", total);
            sucMap.put("diasLabels", diasLabels);
            sucMap.put("diasData", diasData);

            result.add(sucMap);
        }

        return ResponseEntity.ok(result);
    }
}
