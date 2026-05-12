package com.mioptica.controller;

import com.mioptica.model.Proveedor;
import com.mioptica.service.ProveedorService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/proveedores")
@RequiredArgsConstructor
public class ProveedorController {

    private final ProveedorService proveedorService;

    // ─── LISTA ────────────────────────────────────────────────────
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','BODEGUERO','CONTADOR')")
    public String lista(Model model) {
        List<Proveedor> proveedores = proveedorService.listarTodos();
        
        long totalActivos   = proveedores.stream().filter(Proveedor::getActivo).count();
        long totalInactivos = proveedores.stream().filter(p -> !p.getActivo()).count();

        model.addAttribute("proveedores",    proveedores);
        model.addAttribute("totalActivos",   totalActivos);
        model.addAttribute("totalInactivos", totalInactivos);
        model.addAttribute("activePage",     "proveedores");
        
        // Objeto para el modal de nuevo/editar
        model.addAttribute("proveedor", new Proveedor());
        
        return "proveedores/lista";
    }

    // ─── GUARDAR (Desde Modal) ────────────────────────────────────
    @PostMapping("/guardar")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','BODEGUERO')")
    public String guardar(
            @Valid @ModelAttribute("proveedor") Proveedor proveedor,
            BindingResult result,
            RedirectAttributes ra) {

        if (result.hasErrors()) {
            ra.addFlashAttribute("mensajeError", "Error de validación: Verifique los datos ingresados.");
            return "redirect:/proveedores";
        }

        try {
            boolean esNuevo = (proveedor.getIdProveedor() == null);
            proveedorService.guardar(proveedor);
            ra.addFlashAttribute("mensajeOk",
                    esNuevo ? "Proveedor registrado exitosamente."
                            : "Proveedor actualizado correctamente.");
        } catch (Exception e) {
            ra.addFlashAttribute("mensajeError", e.getMessage());
        }

        return "redirect:/proveedores";
    }

    // ─── TOGGLE ACTIVO ────────────────────────────────────────────
    @PostMapping("/toggle/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public String toggle(@PathVariable Integer id, RedirectAttributes ra) {
        try {
            proveedorService.toggleActivo(id);
            ra.addFlashAttribute("mensajeOk", "Estado actualizado correctamente.");
        } catch (Exception e) {
            ra.addFlashAttribute("mensajeError", e.getMessage());
        }
        return "redirect:/proveedores";
    }

    // ─── REST API PARA TOM SELECT ─────────────────────────────────
    @GetMapping("/api/buscar")
    @ResponseBody
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','BODEGUERO','CONTADOR')")
    public List<Map<String, Object>> buscarApi(
            @RequestParam(defaultValue = "") String q) {

        return proveedorService.buscar(q).stream()
                .filter(Proveedor::getActivo)
                .limit(15)
                .map(p -> Map.<String, Object>of(
                        "id",       p.getIdProveedor(),
                        "nombre",   p.getNombre(),
                        "empresa",  p.getEmpresa() != null ? p.getEmpresa() : "",
                        "contacto", p.getContacto() != null ? p.getContacto() : "",
                        "nit",      p.getNit() != null ? p.getNit() : "CF"
                ))
                .collect(Collectors.toList());
    }
}
