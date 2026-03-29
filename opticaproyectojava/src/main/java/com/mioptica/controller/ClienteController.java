package com.mioptica.controller;

import com.mioptica.model.Cliente;
import com.mioptica.service.ClienteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/clientes")
@RequiredArgsConstructor
public class ClienteController {

    private final ClienteService clienteService;

    // ─── LISTA ────────────────────────────────────────────────────
    @GetMapping
    public String lista(
            @RequestParam(value = "q",   defaultValue = "") String q,
            @RequestParam(value = "est", defaultValue = "activo") String est,
            Model model) {

        List<Cliente> clientes;
        if (!q.isBlank()) {
            clientes = clienteService.buscar(q);
        } else if ("inactivo".equals(est)) {
            clientes = clienteService.listarTodos().stream()
                    .filter(c -> !c.getActivo()).collect(Collectors.toList());
        } else if ("todos".equals(est)) {
            clientes = clienteService.listarTodos();
        } else {
            clientes = clienteService.listarActivos();
        }

        long totalActivos   = clientes.stream().filter(Cliente::getActivo).count();
        long totalInactivos = clientes.stream().filter(c -> !c.getActivo()).count();

        model.addAttribute("clientes",       clientes);
        model.addAttribute("totalActivos",   totalActivos);
        model.addAttribute("totalInactivos", totalInactivos);
        model.addAttribute("q",              q);
        model.addAttribute("est",            est);
        model.addAttribute("activePage",     "clientes");
        return "clientes/lista";
    }

    // ─── FORMULARIO NUEVO ─────────────────────────────────────────
    @GetMapping("/nuevo")
    public String nuevo(Model model) {
        model.addAttribute("cliente",    new Cliente());
        model.addAttribute("activePage", "clientes");
        model.addAttribute("editando",   false);
        return "clientes/formulario";
    }

    // ─── FORMULARIO EDITAR ────────────────────────────────────────
    @GetMapping("/editar/{id}")
    public String editar(@PathVariable Integer id, Model model,
                         RedirectAttributes ra) {
        return clienteService.findById(id).map(c -> {
            model.addAttribute("cliente",    c);
            model.addAttribute("activePage", "clientes");
            model.addAttribute("editando",   true);
            return "clientes/formulario";
        }).orElseGet(() -> {
            ra.addFlashAttribute("mensajeError", "Cliente no encontrado.");
            return "redirect:/clientes";
        });
    }

    // ─── GUARDAR ──────────────────────────────────────────────────
    @PostMapping("/guardar")
    public String guardar(
            @Valid @ModelAttribute("cliente") Cliente cliente,
            BindingResult result,
            Model model,
            RedirectAttributes ra) {

        if (result.hasErrors()) {
            model.addAttribute("activePage", "clientes");
            model.addAttribute("editando",   cliente.getIdCliente() != null);
            return "clientes/formulario";
        }

        try {
            boolean esNuevo = (cliente.getIdCliente() == null);
            clienteService.guardar(cliente);
            ra.addFlashAttribute("mensajeOk",
                    esNuevo ? "Cliente registrado exitosamente."
                            : "Cliente actualizado correctamente.");
        } catch (Exception e) {
            model.addAttribute("mensajeError", e.getMessage());
            model.addAttribute("activePage",   "clientes");
            model.addAttribute("editando",     cliente.getIdCliente() != null);
            return "clientes/formulario";
        }

        return "redirect:/clientes";
    }

    // ─── TOGGLE ACTIVO ────────────────────────────────────────────
    @PostMapping("/toggle/{id}")
    public String toggle(@PathVariable Integer id, RedirectAttributes ra) {
        try {
            clienteService.toggleActivo(id);
        } catch (Exception e) {
            ra.addFlashAttribute("mensajeError", e.getMessage());
        }
        return "redirect:/clientes";
    }

    // ─── ELIMINAR ─────────────────────────────────────────────────
    @PostMapping("/eliminar/{id}")
    public String eliminar(@PathVariable Integer id, RedirectAttributes ra) {
        try {
            clienteService.eliminar(id);
            ra.addFlashAttribute("mensajeOk", "Cliente eliminado correctamente.");
        } catch (Exception e) {
            ra.addFlashAttribute("mensajeError", "No se pudo eliminar: " + e.getMessage());
        }
        return "redirect:/clientes";
    }

    // ─── REST API ─────────────────────────────────────────────────
    @GetMapping("/api/buscar")
    @ResponseBody
    public List<Map<String, Object>> buscarApi(
            @RequestParam(defaultValue = "") String q) {

        return clienteService.buscar(q).stream()
                .limit(10)
                .map(c -> Map.<String, Object>of(
                        "id",       c.getIdCliente(),
                        "nombre",   c.getNombre(),
                        "dpi",      c.getDpi()      != null ? c.getDpi()      : "",
                        "nit",      c.getNit()      != null ? c.getNit()      : "CF",
                        "telefono", c.getTelefono() != null ? c.getTelefono() : ""
                ))
                .collect(Collectors.toList());
    }
}
