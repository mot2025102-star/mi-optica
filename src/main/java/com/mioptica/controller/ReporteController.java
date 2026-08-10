package com.mioptica.controller;
 
import com.mioptica.dto.VentaDetalleDTO;
import com.mioptica.model.FichaClinica;
import com.mioptica.model.Inventario;

import com.mioptica.repository.SucursalRepository;
import com.mioptica.repository.UsuarioRepository;
import com.mioptica.repository.VentaRepository;
import com.mioptica.service.CorteCajaService;
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
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import com.mioptica.model.FichaClinica;
import com.mioptica.repository.FichaClinicaRepository;
import com.mioptica.repository.InventarioRepository;


import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
 
@Controller
@RequiredArgsConstructor
public class ReporteController {
 
    private final ReporteService      reporteService;
    private final ExportService       exportService;
    private final UsuarioRepository   usuarioRepo;
    private final SucursalRepository  sucursalRepo;
    private final CorteCajaService    corteCajaService;
    private final FichaClinicaRepository fichaClinicaRepo;
    private final VentaRepository ventaRepo;
    private final InventarioRepository inventarioRepo;
 
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
            @RequestParam(defaultValue = "graficas") String tab,
            @AuthenticationPrincipal UserDetails ud,
            Model model) {
 
        if (fi == null) fi = LocalDate.now().withDayOfMonth(1);
        if (ff == null) ff = LocalDate.now();
 
        var usuario  = usuarioRepo.findByUsername(ud.getUsername()).orElseThrow();
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
 
            Map<String, Object> datos = reporteService.generarReporte(
                    fi, ff, idSucursal, esAdmin, idCategoria, idVendedor);
 
            @SuppressWarnings("unchecked")
            List<VentaDetalleDTO> ventas = (List<VentaDetalleDTO>) datos.get("detalleVentas");
            String nomUsuario = usuario.getNombreCompleto();
            String nomSucursal = idSucursal == 0 ? "Todas las sucursales" : sucursalRepo.findById(idSucursal).map(s -> s.getNombre()).orElse("Desconocida");
            byte[] archivo = exportService.exportarExcel(ventas, fi, ff, nomUsuario, nomSucursal);
 
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
            String nomUsuario = usuario.getNombreCompleto();
            String nomSucursal = idSucursal == 0 ? "Todas las sucursales" : sucursalRepo.findById(idSucursal).map(s -> s.getNombre()).orElse("Desconocida");
            byte[] archivo = exportService.exportarPdf(ventas, fi, ff, nomUsuario, nomSucursal);
 
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=reporte_ventas_" + fi + "_" + ff + ".pdf")
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(archivo);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }
 

    // ─── CORTE DE CAJA — GET ──────────────────────────────────────
    @GetMapping("/corte-caja")
    public String corteCaja(
            @RequestParam(defaultValue = "0") Integer idSucursal,
            @AuthenticationPrincipal UserDetails ud,
            Model model) {

        var usuario = usuarioRepo.findByUsername(ud.getUsername()).orElseThrow();
        boolean esAdmin = usuario.esAdmin();

        if (!esAdmin && usuario.getSucursal() != null)
            idSucursal = usuario.getSucursal().getIdSucursal();
        if (idSucursal == 0 && usuario.getSucursal() != null)
            idSucursal = usuario.getSucursal().getIdSucursal();

        Map<String, Object> corte = reporteService.corteDeCaja(idSucursal);

        model.addAttribute("corte",      corte);
        model.addAttribute("idSucursal", idSucursal);
        model.addAttribute("esAdmin",    esAdmin);
        model.addAttribute("sucursales", sucursalRepo.findByActivoTrue());
        model.addAttribute("sucursal",   sucursalRepo.findById(idSucursal).orElse(null));
        model.addAttribute("activePage", "corte-caja");
        return "reportes/corte";
    }

    // ─── CORTE DE CAJA — POST: nuevo corte ───────────────────────
    @PostMapping("/corte-caja/nuevo")
    public String nuevoCorte(
            @RequestParam Integer idSucursal,
            @AuthenticationPrincipal UserDetails ud,
            RedirectAttributes ra) {
        try {
            corteCajaService.nuevoCorte(idSucursal, ud.getUsername());
            ra.addFlashAttribute("mensajeOk", "Nuevo corte creado.");
        } catch (Exception e) {
            ra.addFlashAttribute("mensajeError", e.getMessage());
        }
        return "redirect:/corte-caja?idSucursal=" + idSucursal;
    }
 
    // ══════════════════════════════════════════════════════════════
    // ─── CORTE DE CAJA — POST: saldo inicial ──────────────────────
    // ══════════════════════════════════════════════════════════════
   @PostMapping("/corte-caja/saldo-inicial")
    public String actualizarSaldoInicial(
            @RequestParam Integer    idCorte,
            @RequestParam BigDecimal saldoInicial,
            @RequestParam Integer    idSucursal,
            RedirectAttributes ra) {
        try {
            corteCajaService.actualizarSaldoInicial(idCorte, saldoInicial);
            ra.addFlashAttribute("mensajeOk", "Saldo inicial actualizado.");
        } catch (Exception e) {
            ra.addFlashAttribute("mensajeError", e.getMessage());
        }
        return "redirect:/corte-caja?idSucursal=" + idSucursal;
    }
 
    // ══════════════════════════════════════════════════════════════
    // ─── CORTE DE CAJA — POST: cerrar corte ───────────────────────
    // ══════════════════════════════════════════════════════════════
    @PostMapping("/corte-caja/cerrar")
    public String cerrarCorte(
            @RequestParam Integer idCorte,
            @RequestParam(required = false) String observacion,
            @RequestParam Integer idSucursal,
            RedirectAttributes ra) {
        try {
            corteCajaService.cerrarCorte(idCorte, null, observacion);
            ra.addFlashAttribute("mensajeOk", "Corte cerrado correctamente.");
        } catch (Exception e) {
            ra.addFlashAttribute("mensajeError", e.getMessage());
        }
        return "redirect:/corte-caja?idSucursal=" + idSucursal;
    }
 
    // ══════════════════════════════════════════════════════════════
    // ─── CORTE DE CAJA — POST: agregar gasto ──────────────────────
    // ══════════════════════════════════════════════════════════════
    @PostMapping("/corte-caja/gasto")
    public String agregarGasto(
            @RequestParam String     concepto,
            @RequestParam BigDecimal monto,
            @RequestParam Integer    idSucursal,
            @AuthenticationPrincipal UserDetails ud,
            RedirectAttributes ra) {
        try {
            corteCajaService.registrarGasto(
                    LocalDate.now(), idSucursal,
                    ud.getUsername(), concepto, monto);
            ra.addFlashAttribute("mensajeOk", "Gasto registrado.");
        } catch (Exception e) {
            ra.addFlashAttribute("mensajeError", e.getMessage());
        }
        return "redirect:/corte-caja?idSucursal=" + idSucursal;
    }
 
    // ══════════════════════════════════════════════════════════════
    // ─── CORTE DE CAJA — POST: eliminar gasto ─────────────────────
    // ══════════════════════════════════════════════════════════════
    @PostMapping("/corte-caja/gasto/eliminar")
    public String eliminarGasto(
            @RequestParam Integer idGasto,
            @RequestParam Integer idSucursal,
            @AuthenticationPrincipal UserDetails ud,
            RedirectAttributes ra) {
        try {
            corteCajaService.eliminarGasto(idGasto, ud.getUsername());
            ra.addFlashAttribute("mensajeOk", "Gasto eliminado.");
        } catch (Exception e) {
            ra.addFlashAttribute("mensajeError", e.getMessage());
        }
        return "redirect:/corte-caja?idSucursal=" + idSucursal;
    }
    // ══════════════════════════════════════════════════════════════
// ─── REPORTES DE FICHAS ───────────────────────────────────────
// ══════════════════════════════════════════════════════════════
@GetMapping("/reportes/fichas")
public String reportesFichas(
        @RequestParam(required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fi,
        @RequestParam(required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate ff,
        @RequestParam(defaultValue = "0") Integer idSucursal,
        @AuthenticationPrincipal UserDetails ud,
        Model model) {

    if (fi == null) fi = LocalDate.now().withDayOfMonth(1);
    if (ff == null) ff = LocalDate.now();

    var usuario = usuarioRepo.findByUsername(ud.getUsername()).orElseThrow();
    boolean esAdmin = usuario.esAdmin();
    if (!esAdmin && usuario.getSucursal() != null)
        idSucursal = usuario.getSucursal().getIdSucursal();

    model.addAttribute("fi",         fi);
    model.addAttribute("ff",         ff);
    model.addAttribute("idSucursal", idSucursal);
    model.addAttribute("esAdmin",    esAdmin);
    model.addAttribute("sucursales", sucursalRepo.findByActivoTrue());
    model.addAttribute("activePage", "reportes");
    return "reportes/fichas";
}

// ─── EXPORTAR FICHAS CLÍNICAS ─────────────────────────────────
@GetMapping("/reportes/fichas/exportar/clinicas")
@ResponseBody
public ResponseEntity<byte[]> exportarFichasClinicas(
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fi,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate ff,
        @RequestParam(defaultValue = "0") Integer idSucursal,
        @AuthenticationPrincipal UserDetails ud) {
    try {
        var usuario = usuarioRepo.findByUsername(ud.getUsername()).orElseThrow();
        boolean esAdmin = usuario.esAdmin();
        if (!esAdmin && usuario.getSucursal() != null)
            idSucursal = usuario.getSucursal().getIdSucursal();

        List<FichaClinica> fichas = idSucursal == 0
                ? fichaClinicaRepo.findByFechaBetweenOrderByFechaDesc(fi, ff)
                : fichaClinicaRepo.findByFechaBetweenAndSucursalOrderByFechaDesc(fi, ff, idSucursal);

        String nomUsuario = usuario.getNombreCompleto();
        String nomSucursal = idSucursal == 0 ? "Todas las sucursales" : sucursalRepo.findById(idSucursal).map(s -> s.getNombre()).orElse("Desconocida");
        byte[] archivo = exportService.exportarFichasClinicas(fichas, fi, ff, nomUsuario, nomSucursal);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=fichas_clinicas_" + fi + "_" + ff + ".xlsx")
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(archivo);
    } catch (Exception e) {
        return ResponseEntity.internalServerError().build();
    }
}

// ─── EXPORTAR SALDOS PENDIENTES ───────────────────────────────
@GetMapping("/reportes/fichas/exportar/saldos")
@ResponseBody
public ResponseEntity<byte[]> exportarSaldosPendientes(
        @RequestParam(defaultValue = "0") Integer idSucursal,
        @AuthenticationPrincipal UserDetails ud) {
    try {
        var usuario = usuarioRepo.findByUsername(ud.getUsername()).orElseThrow();
        boolean esAdmin = usuario.esAdmin();
        if (!esAdmin && usuario.getSucursal() != null)
            idSucursal = usuario.getSucursal().getIdSucursal();

        List<FichaClinica> fichas = idSucursal == 0
                ? fichaClinicaRepo.findConSaldoPendiente()
                : fichaClinicaRepo.findConSaldoPendienteBySucursal(idSucursal);

        String nomUsuario = usuario.getNombreCompleto();
        String nomSucursal = idSucursal == 0 ? "Todas las sucursales" : sucursalRepo.findById(idSucursal).map(s -> s.getNombre()).orElse("Desconocida");
        byte[] archivo = exportService.exportarSaldosPendientes(fichas, nomUsuario, nomSucursal);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=saldos_pendientes.xlsx")
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(archivo);
    } catch (Exception e) {
        return ResponseEntity.internalServerError().build();
    }
}
// ─── EXPORTAR VENTAS POR CLIENTE ──────────────────────────────
@GetMapping("/reportes/fichas/exportar/ventascliente")
@ResponseBody
public ResponseEntity<byte[]> exportarVentasPorCliente(
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fi,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate ff,
        @RequestParam(defaultValue = "0") Integer idSucursal,
        @AuthenticationPrincipal UserDetails ud) {
    try {
        var usuario = usuarioRepo.findByUsername(ud.getUsername()).orElseThrow();
        boolean esAdmin = usuario.esAdmin();
        if (!esAdmin && usuario.getSucursal() != null)
            idSucursal = usuario.getSucursal().getIdSucursal();

        List<Object[]> rows = ventaRepo.detalleVentasPorCliente(fi, ff, idSucursal);
        String nomUsuario = usuario.getNombreCompleto();
        String nomSucursal = idSucursal == 0 ? "Todas las sucursales" : sucursalRepo.findById(idSucursal).map(s -> s.getNombre()).orElse("Desconocida");
        byte[] archivo = exportService.exportarVentasPorCliente(rows, fi, ff, nomUsuario, nomSucursal);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=ventas_por_cliente_" + fi + "_" + ff + ".xlsx")
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(archivo);
    } catch (Exception e) {
        return ResponseEntity.internalServerError().build();
    }
}
// ─── EXPORTAR PRODUCTOS ARMAZONES Y LENTES ────────────────────
@GetMapping("/reportes/fichas/exportar/productos")
@ResponseBody
public ResponseEntity<byte[]> exportarProductos(
        @RequestParam(defaultValue = "0") Integer idSucursal,
        @AuthenticationPrincipal UserDetails ud) {
    try {
        var usuario = usuarioRepo.findByUsername(ud.getUsername()).orElseThrow();
        boolean esAdmin = usuario.esAdmin();
        if (!esAdmin && usuario.getSucursal() != null)
            idSucursal = usuario.getSucursal().getIdSucursal();

        List<Inventario> inventarios = inventarioRepo.findArazonesYLentesBySucursal(idSucursal);
        String nomUsuario = usuario.getNombreCompleto();
        String nomSucursal = idSucursal == 0 ? "Todas las sucursales" : sucursalRepo.findById(idSucursal).map(s -> s.getNombre()).orElse("Desconocida");
        byte[] archivo = exportService.exportarProductosArmazonesLentes(inventarios, nomUsuario, nomSucursal);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=productos_armazones_lentes.xlsx")
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(archivo);
    } catch (Exception e) {
        return ResponseEntity.internalServerError().build();
    }
}
}