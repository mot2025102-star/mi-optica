package com.mioptica.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Entity
@Table(name = "proveedores")
public class Proveedor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_proveedor")
    private Integer idProveedor;

    @NotBlank(message = "El nombre es obligatorio")
    @Column(name = "nombre", nullable = false, length = 100)
    private String nombre;

    @Column(name = "empresa", length = 100)
    private String empresa;

    @Column(name = "contacto", length = 100)
    private String contacto;

    @Column(name = "telefono", length = 20)
    private String telefono;

    @Column(name = "telefono2", length = 20)
    private String telefono2;

    @Column(name = "correo", length = 100)
    private String correo;

    @Column(name = "nit", length = 20)
    private String nit;

    @Column(name = "pais", length = 50)
    private String pais = "Guatemala";

    @Column(name = "direccion", length = 200)
    private String direccion;

    @Column(name = "condicion_pago", length = 30)
    private String condicionPago = "Contado";

    @Column(name = "dias_credito")
    private Integer diasCredito = 0;

    @Column(name = "notas", columnDefinition = "TEXT")
    private String notas;

    @Column(name = "activo", nullable = false)
    private Boolean activo = true;

}
