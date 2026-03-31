package com.mioptica.controller;

import com.mioptica.dto.UsuarioRequest;
import com.mioptica.repository.UsuarioRepository;
import com.mioptica.service.UsuarioService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/usuarios")
@PreAuthorize("hasRole('ADMINISTRADOR')") // Toda esta sección es solo para admin
@RequiredArgsConstructor
public class UsuarioController {

    private final UsuarioService    usuarioService;
    private final UsuarioRepository usuarioRepo;

    // ─── LISTA ────────────────────────────────────────────────────
    @GetMapping
    public String lista(Model model) {
        var usuarios = usuarioService.listarTodos();

        long activos   = usuarios.stream().filter(u ->  u.getActivo()).count();
        long inactivos = usuarios.stream().filter(u -> !u.getActivo()).count();

        model.addAttribute("usuarios",   usuarios);
        model.addAttribute("activos",    activos);
        model.addAttribute("inactivos",  inactivos);
        model.addAttribute("activePage", "usuarios");
        return "usuarios/lista";
    }

    // ─── FORM NUEVO ───────────────────────────────────────────────
    @GetMapping("/nuevo")
    public String nuevo(Model model) {
        model.addAttribute("req",        new UsuarioRequest());
        model.addAttribute("roles",      usuarioService.listarRoles());
        model.addAttribute("sucursales", usuarioService.listarSucursales());
        model.addAttribute("editando",   false);
        model.addAttribute("activePage", "usuarios");
        return "usuarios/formulario";
    }

    // ─── FORM EDITAR ──────────────────────────────────────────────
    @GetMapping("/editar/{id}")
    public String editar(@PathVariable Integer id, Model model, RedirectAttributes ra) {
        return usuarioService.findById(id).map(u -> {
            // Convertir entidad → DTO para pre-rellenar el formulario
            UsuarioRequest req = new UsuarioRequest();
            req.setIdUsuario(u.getIdUsuario());
            req.setNombreCompleto(u.getNombreCompleto());
            req.setUsername(u.getUsername());
            req.setIdRol(u.getRol().getIdRol());
            req.setIdSucursal(u.getSucursal() != null ? u.getSucursal().getIdSucursal() : null);
            req.setPuesto(u.getPuesto());
            req.setActivo(u.getActivo());

            model.addAttribute("req",        req);
            model.addAttribute("roles",      usuarioService.listarRoles());
            model.addAttribute("sucursales", usuarioService.listarSucursales());
            model.addAttribute("editando",   true);
            model.addAttribute("activePage", "usuarios");
            return "usuarios/formulario";
        }).orElseGet(() -> {
            ra.addFlashAttribute("mensajeError", "Usuario no encontrado.");
            return "redirect:/usuarios";
        });
    }

    // ─── GUARDAR NUEVO ────────────────────────────────────────────
    @PostMapping("/crear")
    public String crear(
            @Valid @ModelAttribute("req") UsuarioRequest req,
            BindingResult result,
            Model model,
            RedirectAttributes ra) {

        if (result.hasErrors()) {
            model.addAttribute("roles",      usuarioService.listarRoles());
            model.addAttribute("sucursales", usuarioService.listarSucursales());
            model.addAttribute("editando",   false);
            model.addAttribute("activePage", "usuarios");
            return "usuarios/formulario";
        }

        try {
            var u = usuarioService.crear(req);
            ra.addFlashAttribute("mensajeOk",
                    "Usuario \"" + u.getUsername() + "\" creado exitosamente.");
        } catch (Exception e) {
            model.addAttribute("mensajeError", e.getMessage());
            model.addAttribute("roles",        usuarioService.listarRoles());
            model.addAttribute("sucursales",   usuarioService.listarSucursales());
            model.addAttribute("editando",     false);
            model.addAttribute("activePage",   "usuarios");
            return "usuarios/formulario";
        }

        return "redirect:/usuarios";
    }

    // ─── GUARDAR EDICIÓN ──────────────────────────────────────────
    @PostMapping("/actualizar")
    public String actualizar(
            @Valid @ModelAttribute("req") UsuarioRequest req,
            BindingResult result,
            Model model,
            RedirectAttributes ra) {

        if (result.hasErrors()) {
            model.addAttribute("roles",      usuarioService.listarRoles());
            model.addAttribute("sucursales", usuarioService.listarSucursales());
            model.addAttribute("editando",   true);
            model.addAttribute("activePage", "usuarios");
            return "usuarios/formulario";
        }

        try {
            var u = usuarioService.actualizar(req);
            ra.addFlashAttribute("mensajeOk",
                    "Usuario \"" + u.getUsername() + "\" actualizado.");
        } catch (Exception e) {
            model.addAttribute("mensajeError", e.getMessage());
            model.addAttribute("roles",        usuarioService.listarRoles());
            model.addAttribute("sucursales",   usuarioService.listarSucursales());
            model.addAttribute("editando",     true);
            model.addAttribute("activePage",   "usuarios");
            return "usuarios/formulario";
        }

        return "redirect:/usuarios";
    }

    // ─── TOGGLE ACTIVO ────────────────────────────────────────────
    @PostMapping("/toggle/{id}")
    public String toggle(
            @PathVariable Integer id,
            @AuthenticationPrincipal UserDetails ud,
            RedirectAttributes ra) {

        try {
            var admin = usuarioRepo.findByUsername(ud.getUsername()).orElseThrow();
            usuarioService.toggleActivo(id, admin.getIdUsuario());
        } catch (Exception e) {
            ra.addFlashAttribute("mensajeError", e.getMessage());
        }
        return "redirect:/usuarios";
    }
}
