package com.mioptica.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * DTO para el formulario de crear / editar usuario.
 * No usamos la entidad Usuario directamente porque:
 *  - Al editar NO queremos sobreescribir el password si viene vacío
 *  - Necesitamos validar que las contraseñas coincidan antes de hashear
 */
@Data
public class UsuarioRequest {

    private Integer idUsuario; // null = nuevo, con valor = editar

    @NotBlank(message = "El nombre completo es obligatorio")
    private String nombreCompleto;

    @NotBlank(message = "El username es obligatorio")
    @Size(min = 3, max = 50, message = "El username debe tener entre 3 y 50 caracteres")
    private String username;

    // Solo obligatorio al crear; vacío al editar = no cambiar contraseña
    private String password;
    private String passwordConfirm;

    @NotNull(message = "Debes seleccionar un rol")
    private Integer idRol;

    @NotNull(message = "Debes seleccionar una sucursal")
    private Integer idSucursal;

    private String  puesto;
    private Boolean activo = true;

    // ─── Validaciones de negocio ──────────────────────────────────

    public boolean esNuevo() {
        return idUsuario == null;
    }

    public boolean tienePassword() {
        return password != null && !password.isBlank();
    }

    public boolean passwordsCoinciden() {
        if (!tienePassword()) return true; // no se está cambiando
        return password.equals(passwordConfirm);
    }
}
