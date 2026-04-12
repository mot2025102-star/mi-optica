package com.mioptica.controller;

import com.mioptica.dto.VentaDetalleDTO;
import com.mioptica.repository.CategoriaRepository;
import com.mioptica.repository.SucursalRepository;
import com.mioptica.repository.UsuarioRepository;
import com.mioptica.service.ExportService;
import com.mioptica.service.ReporteService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Controller
@RequiredArgsConstructor
public class ReporteController {

    private final ReporteService       reporteService;
    private final ExportService        exportService;
    private final UsuarioRepository    usuarioRepo;
    private final SucursalRepository   sucursalRepo;
    private final CategoriaRepository  categoriaRepo;

    // ══════════════════════════════════════════════════════════════
    // ─── REPORTES DE VENTAS ───────────────────────────────────────
    // ══════════════════════════════════════════════════════════════
    @GetMapping("/reportes")
    public String reportes(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fi,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate ff,
            @RequestParam(defaultValue = "0") Integer idSucursal,
            @RequestParam(defaultValue = "0") Integer idCategoria,
            @RequestParam(defaultValue = "0") Integer idVendedor,
            @RequestParam(defaultValue = "productos") String tab,
            @AuthenticationPrincipal UserDetails ud,
            Model model) {

        if (fi == null) fi = LocalDate.now().withDayOfMonth(1);
        if (ff == null) ff = LocalDate.now();

        var usuario = usuarioRepo.findByUsername(ud.getUsername()).orElseThrow();
        boolean esAdmin = usuario.esAdmin();

        if (!esAdmin && usuario.getSucursal() != null)
            idSucursal = usuario.getSucursal().getIdSucursal();

        var datos = reporteService.generarReporte(fi, ff, idSucursal, esAdmin,
                                                   idCategoria, idVendedor);

        model.addAttribute("datos",       datos);
        model.addAttribute("fi",          fi);
        model.addAttribute("ff",          ff);
        model.addAttribute("idSucursal",  idSucursal);
        model.addAttribute("idCategoria", idCategoria);
        model.addAttribute("idVendedor",  idVendedor);
        model.addAttribute("tab",         tab);
        model.addAttribute("esAdmin",     esAdmin);
        model.addAttribute("sucursales",  sucursalRepo.findByActivoTrue());
        model.addAttribute("categorias",  categoriaRepo.findAll());
        model.addAttribute("vendedores",  usuarioRepo.findAllByOrderByActivoDescNombreCompletoAsc());
        model.addAttribute("activePage",  "reportes");
        return "reportes/lista";
    }

    // ══════════════════════════════════════════════════════════════
    // ─── EXPORTAR EXCEL ───────────────────────────────────────────
    // ══════════════════════════════════════════════════════════════
    @GetMapping("/reportes/exportar/excel")
    @ResponseBody
    public ResponseEntity<byte[]> exportarExcel(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fi,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate ff,
            @RequestParam(defaultValue = "0") Integer idSucursal,
            @RequestParam(defaultValue = "0") Integer idCategoria,
            @RequestParam(defaultValue = "0") Integer idVendedor,
            @AuthenticationPrincipal UserDetails ud) {

        try {
            var usuario = usuarioRepo.findByUsername(ud.getUsername()).orElseThrow();
            boolean esAdmin = usuario.esAdmin();
            if (!esAdmin && usuario.getSucursal() != null)
                idSucursal = usuario.getSucursal().getIdSucursal();

            // Obtenemos el detalle de ventas
            Map<String, Object> datos = reporteService.generarReporte(
                    fi, ff, idSucursal, esAdmin, idCategoria, idVendedor);

            @SuppressWarnings("unchecked")
            List<VentaDetalleDTO> ventas = (List<VentaDetalleDTO>) datos.get("detalleVentas");

            byte[] archivo = exportService.exportarExcel(ventas, fi, ff);

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=reporte_ventas_" + fi + "_" + ff + ".xlsx")
                    .contentType(MediaType.parseMediaType(
                            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                    .body(archivo);

        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    // ══════════════════════════════════════════════════════════════
    // ─── EXPORTAR PDF ─────────────────────────────────────────────
    // ══════════════════════════════════════════════════════════════
    @GetMapping("/reportes/exportar/pdf")
    @ResponseBody
    public ResponseEntity<byte[]> exportarPdf(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fi,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate ff,
            @RequestParam(defaultValue = "0") Integer idSucursal,
            @RequestParam(defaultValue = "0") Integer idCategoria,
            @RequestParam(defaultValue = "0") Integer idVendedor,
            @AuthenticationPrincipal UserDetails ud) {

        try {
            var usuario = usuarioRepo.findByUsername(ud.getUsername()).orElseThrow();
            boolean esAdmin = usuario.esAdmin();
            if (!esAdmin && usuario.getSucursal() != null)
                idSucursal = usuario.getSucursal().getIdSucursal();

            Map<String, Object> datos = reporteService.generarReporte(
                    fi, ff, idSucursal, esAdmin, idCategoria, idVendedor);

            @SuppressWarnings("unchecked")
            List<VentaDetalleDTO> ventas = (List<VentaDetalleDTO>) datos.get("detalleVentas");

            byte[] archivo = exportService.exportarPdf(ventas, fi, ff);

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=reporte_ventas_" + fi + "_" + ff + ".pdf")
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(archivo);

        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    // ══════════════════════════════════════════════════════════════
    // ─── CORTE DE CAJA ────────────────────────────────────────────
    // ══════════════════════════════════════════════════════════════
    @GetMapping("/corte-caja")
    public String corteCaja(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
            @RequestParam(defaultValue = "0") Integer idSucursal,
            @AuthenticationPrincipal UserDetails ud,
            Model model) {

        if (fecha == null) fecha = LocalDate.now();

        var usuario = usuarioRepo.findByUsername(ud.getUsername()).orElseThrow();
        boolean esAdmin = usuario.esAdmin();

        if (!esAdmin && usuario.getSucursal() != null)
            idSucursal = usuario.getSucursal().getIdSucursal();

        if (idSucursal == 0 && usuario.getSucursal() != null)
            idSucursal = usuario.getSucursal().getIdSucursal();

        var corte = reporteService.corteDeCaja(fecha, idSucursal);

        model.addAttribute("corte",       corte);
        model.addAttribute("fecha",       fecha);
        model.addAttribute("idSucursal",  idSucursal);
        model.addAttribute("esAdmin",     esAdmin);
        model.addAttribute("sucursales",  sucursalRepo.findByActivoTrue());
        model.addAttribute("sucursal",    sucursalRepo.findById(idSucursal).orElse(null));
        model.addAttribute("activePage",  "corte-caja");
        return "reportes/corte";
    }
}