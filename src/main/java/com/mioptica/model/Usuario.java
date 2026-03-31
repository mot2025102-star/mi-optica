package com.mioptica.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Entity
@Table(name = "usuarios")
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_usuario")
    private Integer idUsuario;

    @NotBlank(message = "El nombre es obligatorio")
    @Column(name = "nombre_completo", nullable = false, length = 100)
    private String nombreCompleto;

    @NotBlank(message = "El username es obligatorio")
    @Size(min = 3, max = 50, message = "El username debe tener entre 3 y 50 caracteres")
    @Column(name = "username", nullable = false, unique = true, length = 50)
    private String username;

    // Almacenamos el hash BCrypt, nunca la contraseña en texto plano
    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_rol", nullable = false)
    private Rol rol;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_sucursal")
    private Sucursal sucursal;

    @Column(name = "puesto", length = 100)
    private String puesto;

    @Column(name = "activo", nullable = false)
    private Boolean activo = true;

    // Métodos de conveniencia
    public boolean esAdmin() {
        return rol != null && rol.getIdRol() == 1;
    }

    public boolean esVendedor() {
        return rol != null && rol.getIdRol() == 2;
    }

    public boolean esBodeguero() {
        return rol != null && rol.getIdRol() == 3;
    }

    public boolean esOptometrista() {
        return rol != null && rol.getIdRol() == 4;
    }

    public boolean esContador() {
        return rol != null && rol.getIdRol() == 5;
    }

    public String getNombreCompleto() {
        return nombreCompleto;
    }
}


