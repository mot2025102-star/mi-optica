package com.mioptica.controller;

import com.mioptica.dto.VentaRequest;
import com.mioptica.model.Inventario;
import com.mioptica.model.Venta;
import com.mioptica.repository.InventarioRepository;
import com.mioptica.repository.UsuarioRepository;
import com.mioptica.service.VentaService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/ventas")
@RequiredArgsConstructor
public class VentaController {

    private final VentaService         ventaService;
    private final InventarioRepository inventarioRepo;
    private final UsuarioRepository    usuarioRepo;

    // ─── LISTA + FILTRO POR FECHA ─────────────────────────────────
    @GetMapping
    public String lista(
            @RequestParam(value = "fi",  required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fi,
            @RequestParam(value = "ff",  required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate ff,
            @RequestParam(value = "est", defaultValue = "") String est,
            @AuthenticationPrincipal UserDetails ud,
            Model model) {

        if (fi == null) fi = LocalDate.now().withDayOfMonth(1);
        if (ff == null) ff = LocalDate.now();

        var usuario = usuarioRepo.findByUsername(ud.getUsername()).orElseThrow();
        boolean esAdmin = usuario.esAdmin();
        Integer idSuc = (usuario.getSucursal() != null)
                ? usuario.getSucursal().getIdSucursal()
                : null;

        // Si no tiene sucursal asignada, ver todo
        if (idSuc == null) esAdmin = true;

        var ventas = ventaService.listarPorPeriodo(fi, ff, idSuc, esAdmin);

        if (!est.isBlank()) {
            ventas = ventas.stream()
                    .filter(v -> v.getEstado().equalsIgnoreCase(est))
                    .toList();
        }

        var totalPeriodo = ventas.stream()
                .filter(v -> !"Anulada".equals(v.getEstado()))
                .map(Venta::getTotal)
                .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);

        long cantPeriodo = ventas.stream()
                .filter(v -> !"Anulada".equals(v.getEstado())).count();

        model.addAttribute("ventas",       ventas);
        model.addAttribute("totalPeriodo", totalPeriodo);
        model.addAttribute("cantPeriodo",  cantPeriodo);
        model.addAttribute("fi",           fi);
        model.addAttribute("ff",           ff);
        model.addAttribute("est",          est);
        model.addAttribute("activePage",   "ventas");
        return "ventas/lista";
    }

    // ─── FORMULARIO NUEVA VENTA ───────────────────────────────────
    @GetMapping("/nueva")
    public String nueva(@AuthenticationPrincipal UserDetails ud, Model model) {
        var usuario  = usuarioRepo.findByUsername(ud.getUsername()).orElseThrow();
        Integer idSuc = usuario.getSucursal() != null
                ? usuario.getSucursal().getIdSucursal() : null;

        List<Inventario> stock = idSuc != null
                ? inventarioRepo.findConStockBySucursal(idSuc)
                : inventarioRepo.findTodosConProductoActivo();

        model.addAttribute("stockDisponible", stock);
        model.addAttribute("sucursal",        usuario.getSucursal() != null
                ? usuario.getSucursal().getNombre() : "Sin sucursal");
        model.addAttribute("activePage",      "ventas");
        return "ventas/nueva";
    }

    // ─── REGISTRAR VENTA (recibe JSON del frontend) ───────────────
    @PostMapping("/registrar")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> registrar(
            @RequestBody VentaRequest req,
            @AuthenticationPrincipal UserDetails ud) {

        try {
            var usuario = usuarioRepo.findByUsername(ud.getUsername()).orElseThrow();
            Integer idSuc = usuario.getSucursal() != null
                    ? usuario.getSucursal().getIdSucursal() : null;

            if (idSuc == null) {
                return ResponseEntity.badRequest().body(Map.of(
                        "ok",    false,
                        "error", "El usuario no tiene sucursal asignada."
                ));
            }

            Venta venta = ventaService.registrar(req, idSuc, usuario.getIdUsuario());
            return ResponseEntity.ok(Map.of(
                    "ok",      true,
                    "factura", venta.getNumeroFactura(),
                    "idVenta", venta.getIdVenta(),
                    "total",   venta.getTotal()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "ok",    false,
                    "error", e.getMessage()
            ));
        }
    }

    // ─── DETALLE (para modal) ─────────────────────────────────────
    @GetMapping("/detalle/{id}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> detalle(@PathVariable Integer id) {
        return ventaService.findById(id)
                .map(v -> {
                    var items = v.getDetalles().stream().map(d -> Map.<String, Object>of(
                            "codigo",    d.getProducto().getCodigo(),
                            "detalle",   d.getProducto().getDetalle(),
                            "cantidad",  d.getCantidad(),
                            "precio",    d.getPrecioUnitario(),
                            "descuento", d.getDescuento(),
                            "subtotal",  d.getSubtotal()
                    )).toList();

                    Map<String, Object> info = new java.util.HashMap<>();
                    info.put("factura",     v.getNumeroFactura());
                    info.put("fecha",       v.getFecha().toString());
                    info.put("cliente",     v.getCliente() != null ? v.getCliente().getNombre() : "CF");
                    info.put("nit",         v.getCliente() != null ? (v.getCliente().getNit() != null ? v.getCliente().getNit() : "CF") : "CF");
                    info.put("vendedor",    v.getUsuario().getNombreCompleto());
                    info.put("sucursal",    v.getSucursal().getNombre());
                    info.put("subtotal",    v.getSubtotal());
                    info.put("descuento",   v.getDescuento());
                    info.put("total",       v.getTotal());
                    info.put("estado",      v.getEstado());
                    info.put("observacion", v.getObservacion() != null ? v.getObservacion() : "");

                    return ResponseEntity.ok(Map.<String, Object>of("info", info, "items", items));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    // ─── ANULAR ───────────────────────────────────────────────────
    @PostMapping("/anular/{id}")
    public String anular(@PathVariable Integer id,
                         @AuthenticationPrincipal UserDetails ud,
                         RedirectAttributes ra) {
        try {
            var usuario = usuarioRepo.findByUsername(ud.getUsername()).orElseThrow();
            Integer idSuc = usuario.getSucursal() != null
                    ? usuario.getSucursal().getIdSucursal() : null;
            ventaService.anular(id, idSuc);
            ra.addFlashAttribute("mensajeOk", "Venta anulada. El inventario fue restaurado.");
        } catch (Exception e) {
            ra.addFlashAttribute("mensajeError", e.getMessage());
        }
        return "redirect:/ventas";
    }

    // ─── PDF FACTURA ──────────────────────────────────────────────
    @GetMapping("/factura/{id}")
    public String factura(@PathVariable Integer id, Model model, RedirectAttributes ra) {
        return ventaService.findById(id).map(v -> {
            model.addAttribute("venta", v);
            return "ventas/factura";
        }).orElseGet(() -> {
            ra.addFlashAttribute("mensajeError", "Factura no encontrada.");
            return "redirect:/ventas";
        });
    }
}