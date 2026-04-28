package com.mioptica.config;

import com.mioptica.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
@RequiredArgsConstructor
public class GlobalModelAdvice {

    private final UsuarioRepository usuarioRepo;

    /**
     * Inyecta el usuario autenticado en TODOS los modelos automáticamente.
     * El layout.html lo usa como ${usuarioPerfil} para mostrar la foto
     * y las iniciales en el topbar.
     */
    @ModelAttribute("usuarioPerfil")
    public com.mioptica.model.Usuario usuarioPerfil(
            @AuthenticationPrincipal UserDetails ud) {
        if (ud == null) return null;
        return usuarioRepo.findByUsername(ud.getUsername()).orElse(null);
    }
}