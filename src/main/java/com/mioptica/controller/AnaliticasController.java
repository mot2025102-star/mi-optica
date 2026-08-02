package com.mioptica.controller;

import com.mioptica.repository.ReporteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import com.mioptica.repository.UsuarioRepository;

import java.time.LocalDate;
import java.util.*;

@RestController
@RequestMapping("/ventas/analiticas")
@RequiredArgsConstructor
public class AnaliticasController {

    private final ReporteRepository reporteRepo;
    private final UsuarioRepository usuarioRepo;

    // ── Helpers ────────────────────────────────────────────────────
    private Integer resolveIdSuc(UserDetails ud) {
        var usuario = usuarioRepo.findByUsername(ud.getUsername()).orElseThrow();
        if (usuario.esAdmin()) return 0; // 0 = todas
        return usuario.getSucursal() != null ? usuario.getSucursal().getIdSucursal() : 0;
    }

    // ── 1. Ventas por día ──────────────────────────────────────────
    @GetMapping("/por-dia")
    public ResponseEntity<Map<String, Object>> porDia(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fi,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate ff,
            @AuthenticationPrincipal UserDetails ud) {

        int idSuc = resolveIdSuc(ud);
        var rows = reporteRepo.ventasPorDia(fi, ff, idSuc);

        List<String> labels = new ArrayList<>();
        List<Number> totales = new ArrayList<>();

        for (Object[] r : rows) {
            labels.add(r[0].toString());   // fecha yyyy-MM-dd
            totales.add(r[2] != null ? (Number) r[2] : 0);
        }

        return ResponseEntity.ok(Map.of("labels", labels, "data", totales));
    }

    // ── 2. Ventas por día de la semana ─────────────────────────────
    @GetMapping("/por-dia-semana")
    public ResponseEntity<Map<String, Object>> porDiaSemana(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fi,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate ff,
            @AuthenticationPrincipal UserDetails ud) {

        int idSuc = resolveIdSuc(ud);
        var rows = reporteRepo.ventasPorDiaSemana(fi, ff, idSuc);

        // MySQL: DAYOFWEEK 1=Dom, 2=Lun … 7=Sáb
        String[] nombres = {"Dom", "Lun", "Mar", "Mié", "Jue", "Vie", "Sáb"};
        double[] valores = new double[7];
        for (Object[] r : rows) {
            int dow = ((Number) r[0]).intValue(); // 1-7
            double total = r[1] != null ? ((Number) r[1]).doubleValue() : 0;
            if (dow >= 1 && dow <= 7) valores[dow - 1] += total;
        }

        // Reordenar Lun→Dom (para mostrar semana laboral primero)
        String[] labelsOrdenados = {"Lun", "Mar", "Mié", "Jue", "Vie", "Sáb", "Dom"};
        double[] datosOrdenados  = {valores[1], valores[2], valores[3], valores[4], valores[5], valores[6], valores[0]};

        return ResponseEntity.ok(Map.of("labels", labelsOrdenados, "data", datosOrdenados));
    }

    // ── 3. Ventas por mes ──────────────────────────────────────────
    @GetMapping("/por-mes")
    public ResponseEntity<Map<String, Object>> porMes(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fi,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate ff,
            @AuthenticationPrincipal UserDetails ud) {

        int idSuc = resolveIdSuc(ud);
        // Para ventas por mes siempre usamos rango amplio (inicio del año hasta hoy)
        LocalDate fiMes = fi.withDayOfYear(1).minusYears(1);
        var rows = reporteRepo.ventasPorMes(fiMes, ff, idSuc);

        List<String> labels = new ArrayList<>();
        List<Number> totales = new ArrayList<>();
        String[] meses = {"Ene","Feb","Mar","Abr","May","Jun","Jul","Ago","Sep","Oct","Nov","Dic"};

        for (Object[] r : rows) {
            String yearMes = r[0].toString(); // "2026-07"
            String[] parts = yearMes.split("-");
            int m = Integer.parseInt(parts[1]) - 1;
            labels.add(meses[m] + " " + parts[0]);
            totales.add(r[2] != null ? (Number) r[2] : 0);
        }

        return ResponseEntity.ok(Map.of("labels", labels, "data", totales));
    }

    // ── 4. Top productos por cantidad vendida ──────────────────────
    @GetMapping("/top-productos")
    public ResponseEntity<Map<String, Object>> topProductos(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fi,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate ff,
            @AuthenticationPrincipal UserDetails ud) {

        int idSuc = resolveIdSuc(ud);
        var rows = reporteRepo.topProductosPorCantidad(fi, ff, idSuc);

        List<String> labels = new ArrayList<>();
        List<Number> cantidades = new ArrayList<>();

        for (Object[] r : rows) {
            String nombre = r[0] != null ? r[0].toString() : "—";
            // Truncar nombre largo para que quepa en la gráfica
            if (nombre.length() > 35) nombre = nombre.substring(0, 33) + "…";
            labels.add(nombre);
            cantidades.add(r[1] != null ? (Number) r[1] : 0);
        }

        return ResponseEntity.ok(Map.of("labels", labels, "data", cantidades));
    }

    // ── 5. Comparación entre sucursales ───────────────────────────
    @GetMapping("/por-sucursal")
    public ResponseEntity<List<Map<String, Object>>> porSucursal(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fi,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate ff) {

        var resumen = reporteRepo.resumenPorSucursal(fi, ff);
        List<Map<String, Object>> resultado = new ArrayList<>();

        for (Object[] r : resumen) {
            int idSuc = ((Number) r[0]).intValue();
            String nombre = r[1] != null ? r[1].toString() : "Sucursal";
            long facturas = r[2] != null ? ((Number) r[2]).longValue() : 0;
            double total  = r[3] != null ? ((Number) r[3]).doubleValue() : 0;

            // Mini gráfica: ventas por día de esa sucursal
            var diasRows = reporteRepo.ventasPorDiaPorSucursal(fi, ff, idSuc);
            List<String> diasLabels = new ArrayList<>();
            List<Number> diasData   = new ArrayList<>();
            for (Object[] d : diasRows) {
                diasLabels.add(d[0].toString());
                diasData.add(d[1] != null ? (Number) d[1] : 0);
            }

            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("idSucursal", idSuc);
            entry.put("nombre", nombre);
            entry.put("facturas", facturas);
            entry.put("total", total);
            entry.put("diasLabels", diasLabels);
            entry.put("diasData", diasData);
            resultado.add(entry);
        }

        return ResponseEntity.ok(resultado);
    }
}
