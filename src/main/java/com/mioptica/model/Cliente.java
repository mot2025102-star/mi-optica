package com.mioptica.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@Entity
@Table(name = "clientes")
public class Cliente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_cliente")
    private Integer idCliente;

    @NotBlank(message = "El nombre es obligatorio")
    @Column(name = "nombre", nullable = false, length = 100)
    private String nombre;

    @Column(name = "dpi", length = 20, unique = true)
    private String dpi;

    @Column(name = "nit", length = 20)
    private String nit = "CF";

    @Column(name = "telefono", length = 20)
    private String telefono;

    @Column(name = "telefono2", length = 20)
    private String telefono2;

    @Column(name = "correo", length = 100)
    private String correo;

    @Column(name = "fecha_nacimiento")
    private LocalDate fechaNacimiento;

    @Column(name = "sexo", length = 20)
    private String sexo;

    @Column(name = "ocupacion", length = 100)
    private String ocupacion;

    @Column(name = "direccion", length = 200)
    private String direccion;

    @Column(name = "zona", length = 10)
    private String zona;

    @Column(name = "municipio", length = 80)
    private String municipio;

    @Column(name = "notas", columnDefinition = "TEXT")
    private String notas;

    @Column(name = "activo", nullable = false)
    private Boolean activo = true;

    // ── DATOS EMPRESARIALES ───────────────────────────────────────────
    @Column(name = "empresa_nombre",   length = 150)
    private String empresaNombre;

    @Column(name = "empresa_contacto", length = 100)
    private String empresaContacto;

    @Column(name = "empresa_telefono", length = 20)
    private String empresaTelefono;

    @Column(name = "empresa_correo",   length = 100)
    private String empresaCorreo;

    @Column(name = "empresa_departamento", length = 80)
    private String empresaDepartamento;

    @Column(name = "empresa_municipio", length = 80)
    private String empresaMunicipio;

    @Column(name = "empresa_zona", length = 10)
    private String empresaZona;

    // Calcula edad en años
    public Integer getEdad() {
        if (fechaNacimiento == null) return null;
        return LocalDate.now().getYear() - fechaNacimiento.getYear();
    }
}
