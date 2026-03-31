package com.mioptica.controller;

import com.mioptica.repository.SucursalRepository;
import com.mioptica.repository.UsuarioRepository;
import com.mioptica.service.ReporteService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.YearMonth;

@Controller
@RequiredArgsConstructor
public class ReporteController {

    private final ReporteService     reporteService;
    private final UsuarioRepository  usuarioRepo;
    private final SucursalRepository sucursalRepo;

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
            @RequestParam(defaultValue = "productos") String tab,
            @AuthenticationPrincipal UserDetails ud,
            Model model) {

        // Fechas por defecto: mes actual
        if (fi == null) fi = LocalDate.now().withDayOfMonth(1);
        if (ff == null) ff = LocalDate.now();

        var usuario = usuarioRepo.findByUsername(ud.getUsername()).orElseThrow();
        boolean esAdmin = usuario.esAdmin();

        // Admin puede filtrar sucursal; otros solo ven la suya
        if (!esAdmin) idSucursal = usuario.getSucursal().getIdSucursal();

        var datos = reporteService.generarReporte(fi, ff, idSucursal, esAdmin);

        model.addAttribute("datos",       datos);
        model.addAttribute("fi",          fi);
        model.addAttribute("ff",          ff);
        model.addAttribute("idSucursal",  idSucursal);
        model.addAttribute("tab",         tab);
        model.addAttribute("esAdmin",     esAdmin);
        model.addAttribute("sucursales",  sucursalRepo.findByActivoTrue());
        model.addAttribute("activePage",  "reportes");
        return "reportes/lista";
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

        if (!esAdmin) idSucursal = usuario.getSucursal().getIdSucursal();

        // Si admin no seleccionó, usar su sucursal
        if (idSucursal == 0) idSucursal = usuario.getSucursal().getIdSucursal();

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