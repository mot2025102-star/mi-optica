package com.mioptica.security;

import com.mioptica.model.Usuario;
import com.mioptica.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UsuarioDetailsService implements UserDetailsService {

    private final UsuarioRepository usuarioRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // 1. Buscamos al usuario en la base de datos
        Usuario usuario = usuarioRepository.findByUsernameAndActivoTrue(username)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado: " + username));

        // 2. Generamos el nombre del rol (Ej: ROLE_ADMINISTRADOR)
        String autoridad = "ROLE_" + usuario.getRol().getNombre()
                .toUpperCase(java.util.Locale.ROOT).replace(" ", "_");

        // ─── BLOQUE DE DIAGNÓSTICO (Esto saldrá en tu terminal) ──────────────
        System.out.println("\n==========================================");
        System.out.println("   🔍 DIAGNÓSTICO DE LOGIN EN CURSO");
        System.out.println("==========================================");
        System.out.println("👤 Usuario detectado: " + usuario.getUsername());
        System.out.println("🛡️ Rol en BD: " + usuario.getRol().getNombre());
        System.out.println("🔑 Autoridad enviada a Spring: " + autoridad);
        System.out.println("📝 Hash en BD: " + usuario.getPasswordHash());
        System.out.println("📏 Longitud del Hash: " + (usuario.getPasswordHash() != null ? usuario.getPasswordHash().length() : 0));
        System.out.println("==========================================\n");
        // ─────────────────────────────────────────────────────────────────────

        // 3. Retornamos el usuario a Spring Security para que valide la contraseña
        return new org.springframework.security.core.userdetails.User(
                usuario.getUsername(),
                usuario.getPasswordHash(),
                List.of(new SimpleGrantedAuthority(autoridad))
        );
    }
}