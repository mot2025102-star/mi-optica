package com.mioptica.controller;

import com.mioptica.model.Usuario;
import com.mioptica.repository.InventarioRepository;
import com.mioptica.repository.UsuarioRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.csrf.CsrfToken;
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
            HttpServletRequest  request,
            HttpServletResponse response,
            Model model) {

        /*
         * FIX: En Spring Boot 3.2 + Spring Security 6.2, el CSRF token se carga
         * de forma "diferida" (deferred). Esto significa que la cookie XSRF-TOKEN
         * NO se escribe en la respuesta del GET /login a menos que algo fuerce
         * la carga del token explícitamente.
         *
         * Si la cookie no existe cuando el usuario hace POST, Spring Security
         * rechaza el submit con 403 silencioso. En el segundo intento, la cookie
         * ya está y funciona — ese era el bug del "doble clic".
         *
         * Solución: forzar el token aquí para que Spring lo escriba en la cookie
         * durante este GET, antes de que el usuario vea el formulario.
         */
        CsrfToken csrfToken = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
        if (csrfToken != null) {
            csrfToken.getToken(); // Fuerza la carga y escritura de la cookie XSRF-TOKEN
        }

        if (error   != null) model.addAttribute("mensajeError",   "Usuario o contraseña incorrectos.");
        if (logout  != null) model.addAttribute("mensajeLogout",  "Sesión cerrada correctamente.");
        if (expired != null) model.addAttribute("mensajeExpired", "Tu sesión ha expirado.");

        return "login";
    }

    // Redirige "/" al dashboard unificado
    @GetMapping("/")
    public String root(@AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) return "redirect:/login";
        return "redirect:/dashboard";
    }
}