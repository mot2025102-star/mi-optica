package com.mioptica.controller;

import com.mioptica.model.Usuario;
import com.mioptica.repository.InventarioRepository;
import com.mioptica.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequiredArgsConstructor
public class AuthController {

    private final UsuarioRepository usuarioRepo;
    private final InventarioRepository inventarioRepo;

    // ─── Login ───────────────────────────────────────────────────
    @GetMapping("/login")
    public String login(
            @RequestParam(value = "error",   required = false) String error,
            @RequestParam(value = "logout",  required = false) String logout,
            @RequestParam(value = "expired", required = false) String expired,
            Model model) {

        if (error   != null) model.addAttribute("mensajeError",   "Usuario o contraseña incorrectos.");
        if (logout  != null) model.addAttribute("mensajeLogout",  "Sesión cerrada correctamente.");
        if (expired != null) model.addAttribute("mensajeExpired", "Tu sesión ha expirado.");

        return "login";
    }

    // ─── Dashboards por rol ───────────────────────────────────────
    @GetMapping("/dashboard/admin")
    public String dashboardAdmin(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        Usuario usuario = usuarioRepo.findByUsername(userDetails.getUsername()).orElseThrow();
        model.addAttribute("usuario", usuario);
        model.addAttribute("stockBajo", usuario.getSucursal() != null ? inventarioRepo.findStockBajoBySucursal(usuario.getSucursal().getIdSucursal()) : java.util.List.of());
        return "dashboard/admin";
    }

    @GetMapping("/dashboard/vendedor")
    public String dashboardVendedor(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        Usuario usuario = usuarioRepo.findByUsername(userDetails.getUsername()).orElseThrow();
        model.addAttribute("usuario", usuario);
        model.addAttribute("stockBajo", usuario.getSucursal() != null ? inventarioRepo.findStockBajoBySucursal(usuario.getSucursal().getIdSucursal()) : java.util.List.of());
        return "dashboard/vendedor";
    }

    @GetMapping("/dashboard/bodeguero")
    public String dashboardBodeguero(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        Usuario usuario = usuarioRepo.findByUsername(userDetails.getUsername()).orElseThrow();
        model.addAttribute("usuario", usuario);
        return "dashboard/bodeguero";
    }

    @GetMapping("/dashboard/optometrista")
    public String dashboardOptometrista(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        Usuario usuario = usuarioRepo.findByUsername(userDetails.getUsername()).orElseThrow();
        model.addAttribute("usuario", usuario);
        return "dashboard/optometrista";
    }

    @GetMapping("/dashboard/contador")
    public String dashboardContador(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        Usuario usuario = usuarioRepo.findByUsername(userDetails.getUsername()).orElseThrow();
        model.addAttribute("usuario", usuario);
        return "dashboard/contador";
    }

    // Redirige "/" al dashboard correcto
    @GetMapping("/")
    public String root(@AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) return "redirect:/login";
        String rol = userDetails.getAuthorities().iterator().next().getAuthority();
        return "redirect:/dashboard/" + switch (rol) {
            case "ROLE_ADMINISTRADOR" -> "admin";
            case "ROLE_VENDEDOR"      -> "vendedor";
            case "ROLE_BODEGUERO"     -> "bodeguero";
            case "ROLE_OPTOMETRISTA"  -> "optometrista";
            case "ROLE_CONTADOR"      -> "contador";
            default -> "admin";
        };
    }
}