package com.mioptica.controller;

import com.mioptica.model.FichaClinica;
import com.mioptica.repository.ClienteRepository;
import com.mioptica.repository.SucursalRepository;
import com.mioptica.repository.UsuarioRepository;
import com.mioptica.service.FichaClinicaService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Controller
@RequestMapping("/fichas")
@RequiredArgsConstructor
public class FichaClinicaController {

    private final FichaClinicaService fichaService;
    private final ClienteRepository   clienteRepo;
    private final SucursalRepository  sucursalRepo;
    private final UsuarioRepository   usuarioRepo;

    // ─── LISTA ────────────────────────────────────────────────────
    @GetMapping
    public String lista(
            @RequestParam(defaultValue = "") String q,
            @AuthenticationPrincipal UserDetails ud,
            Model model) {

        var usuario = usuarioRepo.findByUsername(ud.getUsername()).orElseThrow();
        boolean esAdmin = usuario.esAdmin();

        List<FichaClinica> fichas = fichaService.listarTodas();

        // Filtrar por nombre de cliente si hay búsqueda
        if (!q.isBlank()) {
            String qLower = q.toLowerCase();
            fichas = fichas.stream()
                    .filter(f -> f.getCliente().getNombre().toLowerCase().contains(qLower))
                    .toList();
        }
        long entregasPendientes = fichas.stream()
            .filter(f -> !"Entregado".equals(f.getEstadoEntrega()) && f.getFechaEntrega() != null)
            .count();
        long conSaldo = fichas.stream()
            .filter(f -> f.getSaldo() != null && f.getSaldo().compareTo(BigDecimal.ZERO) > 0)
            .count();

        model.addAttribute("fichas",     fichas);
        model.addAttribute("q",          q);
        model.addAttribute("esAdmin",    esAdmin);
        model.addAttribute("activePage", "fichas");
        return "fichas/lista";
    }

    // ─── FORMULARIO NUEVA FICHA ───────────────────────────────────
    @GetMapping("/nueva")
    public String nueva(
            @RequestParam(required = false) Integer idCliente,
            @AuthenticationPrincipal UserDetails ud,
            Model model) {

        var usuario = usuarioRepo.findByUsername(ud.getUsername()).orElseThrow();

        FichaClinica ficha = new FichaClinica();
        ficha.setFecha(LocalDate.now());
        ficha.setOptometrista(usuario);

        // Si viene de un cliente específico, prellenar
        if (idCliente != null) {
            clienteRepo.findById(idCliente).ifPresent(ficha::setCliente);
        }

        model.addAttribute("ficha",       ficha);
        model.addAttribute("clientes",    clienteRepo.findByActivoTrueOrderByNombreAsc());
        model.addAttribute("sucursales",  sucursalRepo.findByActivoTrue());
        model.addAttribute("optometras",  usuarioRepo.findAll());
        model.addAttribute("editando",    false);
        model.addAttribute("activePage",  "fichas");
        return "fichas/formulario";
    }

    // ─── FORMULARIO EDITAR ────────────────────────────────────────
    @GetMapping("/editar/{id}")
    public String editar(@PathVariable Integer id, Model model, RedirectAttributes ra) {
        return fichaService.findById(id).map(f -> {
            model.addAttribute("ficha",      f);
            model.addAttribute("clientes",   clienteRepo.findByActivoTrueOrderByNombreAsc());
            model.addAttribute("sucursales", sucursalRepo.findByActivoTrue());
            model.addAttribute("optometras", usuarioRepo.findAll());
            model.addAttribute("editando",   true);
            model.addAttribute("activePage", "fichas");
            return "fichas/formulario";
        }).orElseGet(() -> {
            ra.addFlashAttribute("mensajeError", "Ficha no encontrada.");
            return "redirect:/fichas";
        });
    }

    // ─── GUARDAR ──────────────────────────────────────────────────
    @PostMapping("/guardar")
    public String guardar(
            @ModelAttribute("ficha") FichaClinica ficha,
            @AuthenticationPrincipal UserDetails ud,
            RedirectAttributes ra) {

        try {
            // Asignar relaciones desde los IDs del form
            fichaService.guardar(ficha);
            ra.addFlashAttribute("mensajeOk",
                    ficha.getIdFicha() == null
                    ? "Ficha clínica creada correctamente."
                    : "Ficha clínica actualizada correctamente.");
        } catch (Exception e) {
            ra.addFlashAttribute("mensajeError", e.getMessage());
        }
        return "redirect:/fichas";
    }

    // ─── ELIMINAR ─────────────────────────────────────────────────
    @PostMapping("/eliminar/{id}")
    public String eliminar(@PathVariable Integer id, RedirectAttributes ra) {
        try {
            fichaService.eliminar(id);
            ra.addFlashAttribute("mensajeOk", "Ficha eliminada.");
        } catch (Exception e) {
            ra.addFlashAttribute("mensajeError", e.getMessage());
        }
        return "redirect:/fichas";
    }
}