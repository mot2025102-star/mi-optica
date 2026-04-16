package com.mioptica.controller;

import com.mioptica.dto.OrdenLabRequest;
import com.mioptica.model.Cliente;
import com.mioptica.model.OrdenLaboratorio;
import com.mioptica.repository.ClienteRepository;
import com.mioptica.repository.UsuarioRepository;
import com.mioptica.service.OrdenLaboratorioService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/ordenes-lab")
@RequiredArgsConstructor
// ✅ Solo OPTOMETRISTA y VENDEDOR (y ADMINISTRADOR si quieres que también vea)
@PreAuthorize("hasAnyRole('OPTOMETRISTA','VENDEDOR','ADMINISTRADOR')")
public class OrdenLaboratorioController {

    private final OrdenLaboratorioService ordenService;
    private final UsuarioRepository       usuarioRepo;
    private final ClienteRepository       clienteRepo;

    // ── LISTA ─────────────────────────────────────────────────────
    @GetMapping
    public String lista(
            @RequestParam(value = "fi",  required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fi,
            @RequestParam(value = "ff",  required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate ff,
            @RequestParam(value = "estado", defaultValue = "") String estado,
            @AuthenticationPrincipal UserDetails ud,
            Model model) {

        if (fi == null) fi = LocalDate.now().withDayOfMonth(1);
        if (ff == null) ff = LocalDate.now();

        var usuario  = usuarioRepo.findByUsername(ud.getUsername()).orElseThrow();
        boolean esAdmin = usuario.esAdmin();
        Integer idSuc   = usuario.getSucursal() != null
                ? usuario.getSucursal().getIdSucursal() : null;
        if (idSuc == null) esAdmin = true;

        var ordenes = ordenService.listar(fi, ff, idSuc, esAdmin);

        if (!estado.isBlank())
            ordenes = ordenes.stream()
                    .filter(o -> o.getEstado().equalsIgnoreCase(estado))
                    .toList();

        model.addAttribute("ordenes",    ordenes);
        model.addAttribute("fi",         fi);
        model.addAttribute("ff",         ff);
        model.addAttribute("estadoFiltro", estado);
        model.addAttribute("activePage", "ordenes-lab");
        return "ordenes-lab/lista";
    }

    // ── FORMULARIO NUEVA ORDEN ────────────────────────────────────
    @GetMapping("/nueva")
    public String nueva(@AuthenticationPrincipal UserDetails ud, Model model) {
        List<Cliente> clientes = clienteRepo.findAll();
        model.addAttribute("clientes",   clientes);
        model.addAttribute("activePage", "ordenes-lab");
        return "ordenes-lab/nueva";
    }

    // ── REGISTRAR (JSON desde el frontend) ────────────────────────
    @PostMapping("/registrar")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> registrar(
            @RequestBody OrdenLabRequest req,
            @AuthenticationPrincipal UserDetails ud) {
        try {
            var usuario = usuarioRepo.findByUsername(ud.getUsername()).orElseThrow();
            Integer idSuc = usuario.getSucursal() != null
                    ? usuario.getSucursal().getIdSucursal() : null;

            if (idSuc == null)
                return ResponseEntity.badRequest().body(Map.of(
                        "ok", false, "error", "Usuario sin sucursal asignada."));

            var orden = ordenService.crear(req, idSuc, usuario.getIdUsuario());
            return ResponseEntity.ok(Map.of(
                    "ok",      true,
                    "numero",  orden.getNumeroOrden(),
                    "idOrden", orden.getIdOrden()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "ok", false, "error", e.getMessage()));
        }
    }

    // ── DETALLE (para modal) ──────────────────────────────────────
    @GetMapping("/detalle/{id}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> detalle(@PathVariable Integer id) {
        return ordenService.findById(id).map(o -> {
            var prods = o.getProductos().stream().map(p -> Map.<String, Object>of(
                    "codigo",      p.getCodigo()      != null ? p.getCodigo()      : "",
                    "cantidad",    p.getCantidad(),
                    "descripcion", p.getDescripcion() != null ? p.getDescripcion() : "",
                    "material",    p.getMaterial()    != null ? p.getMaterial()    : "",
                    "tratamiento", p.getTratamiento() != null ? p.getTratamiento() : "",
                    "colorTinte",  p.getColorTinte()  != null ? p.getColorTinte()  : ""
            )).toList();

            Map<String, Object> info = new java.util.LinkedHashMap<>();
            info.put("numero",    o.getNumeroOrden());
            info.put("cliente",   o.getCliente() != null ? o.getCliente().getNombre() : "CF");
            info.put("usuario",   o.getUsuario().getNombreCompleto());
            info.put("sucursal",  o.getSucursal().getNombre());
            info.put("fecha",     o.getFechaEmision().toString());
            info.put("estado",    o.getEstado());
            // OD / OI
            info.put("odEsfera",   nv(o.getOdEsfera()));   info.put("odCilindro", nv(o.getOdCilindro()));
            info.put("odEje",      nv(o.getOdEje()));       info.put("odAdd",      nv(o.getOdAdd()));
            info.put("odDip",      nv(o.getOdDip()));       info.put("odNdpod",    nv(o.getOdNdpod()));
            info.put("odNdpoi",    nv(o.getOdNdpoi()));     info.put("odAltura",   nv(o.getOdAltura()));
            info.put("oiEsfera",   nv(o.getOiEsfera()));   info.put("oiCilindro", nv(o.getOiCilindro()));
            info.put("oiEje",      nv(o.getOiEje()));       info.put("oiAdd",      nv(o.getOiAdd()));
            info.put("oiDip",      nv(o.getOiDip()));       info.put("oiNdpod",    nv(o.getOiNdpod()));
            info.put("oiNdpoi",    nv(o.getOiNdpoi()));     info.put("oiAltura",   nv(o.getOiAltura()));
            info.put("pantoscopico", nv(o.getPantoscopico()));
            info.put("vertex",       nv(o.getVertex()));
            info.put("panoramico",   nv(o.getPanoramico()));
            info.put("envioLab",     o.getFechaEnvioLab() != null ? o.getFechaEnvioLab().toString() : "");
            info.put("entregaEst",   o.getFechaEntregaEstimada() != null ? o.getFechaEntregaEstimada().toString() : "");
            info.put("observaciones", nv(o.getObservaciones()));

            return ResponseEntity.ok(Map.<String, Object>of("info", info, "productos", prods));
        }).orElse(ResponseEntity.notFound().build());
    }

    // ── CAMBIAR ESTADO ────────────────────────────────────────────
    @PostMapping("/estado/{id}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> cambiarEstado(
            @PathVariable Integer id,
            @RequestParam String estado) {
        try {
            ordenService.cambiarEstado(id, estado);
            return ResponseEntity.ok(Map.of("ok", true));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("ok", false, "error", e.getMessage()));
        }
    }

    private String nv(String s) { return s != null ? s : ""; }
}
