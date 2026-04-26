package com.mioptica.controller;

import com.mioptica.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.nio.file.*;
import java.util.Map;
import java.util.UUID;

@Controller
@RequestMapping("/perfil")
@RequiredArgsConstructor
public class PerfilController {

    private final UsuarioRepository usuarioRepo;

    // Carpeta donde se guardan las fotos — dentro de static/img/perfiles/
    // Spring Boot sirve todo lo que esté en src/main/resources/static/ como recursos estáticos
    private static final String UPLOAD_DIR =
            "src/main/resources/static/img/perfiles/";

    // ── VER PERFIL ────────────────────────────────────────────────
    @GetMapping
    public String verPerfil(@AuthenticationPrincipal UserDetails ud, Model model) {
        var usuario = usuarioRepo.findByUsername(ud.getUsername())
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        model.addAttribute("usuario",    usuario);
        model.addAttribute("activePage", "perfil");
        return "perfil/ver";
    }

    // ── SUBIR FOTO ────────────────────────────────────────────────
    @PostMapping("/foto")
    public String subirFoto(
            @AuthenticationPrincipal UserDetails ud,
            @RequestParam("foto") MultipartFile foto,
            RedirectAttributes ra) {

        if (foto.isEmpty()) {
            ra.addFlashAttribute("mensajeError", "Debes seleccionar una imagen.");
            return "redirect:/perfil";
        }

        // Validar tipo de archivo
        String contentType = foto.getContentType();
        if (contentType == null ||
            (!contentType.equals("image/jpeg") &&
             !contentType.equals("image/png")  &&
             !contentType.equals("image/webp"))) {
            ra.addFlashAttribute("mensajeError", "Solo se permiten imágenes JPG, PNG o WEBP.");
            return "redirect:/perfil";
        }

        // Validar tamaño máximo 2 MB
        if (foto.getSize() > 2 * 1024 * 1024) {
            ra.addFlashAttribute("mensajeError", "La imagen no puede superar 2 MB.");
            return "redirect:/perfil";
        }

        try {
            var usuario = usuarioRepo.findByUsername(ud.getUsername())
                    .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

            // Crear carpeta si no existe
            Path uploadPath = Paths.get(UPLOAD_DIR);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            // Borrar foto anterior si existe
            if (usuario.getFotoPerfil() != null && !usuario.getFotoPerfil().isBlank()) {
                Path anterior = uploadPath.resolve(usuario.getFotoPerfil());
                Files.deleteIfExists(anterior);
            }

            // Generar nombre único para evitar colisiones
            String extension = contentType.equals("image/png")  ? ".png"  :
                               contentType.equals("image/webp") ? ".webp" : ".jpg";
            String nombreArchivo = "user_" + usuario.getIdUsuario() + "_" +
                                   UUID.randomUUID().toString().substring(0, 8) + extension;

            // Guardar archivo
            Path destino = uploadPath.resolve(nombreArchivo);
            Files.copy(foto.getInputStream(), destino, StandardCopyOption.REPLACE_EXISTING);

            // Actualizar en base de datos
            usuario.setFotoPerfil(nombreArchivo);
            usuarioRepo.save(usuario);

            ra.addFlashAttribute("mensajeOk", "Foto de perfil actualizada correctamente.");

        } catch (IOException e) {
            ra.addFlashAttribute("mensajeError", "Error al guardar la imagen: " + e.getMessage());
        }

        return "redirect:/perfil";
    }
}