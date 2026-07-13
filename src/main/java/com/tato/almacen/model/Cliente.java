package com.tato.almacen.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "clientes")
@Getter
@Setter
public class Cliente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tipo_documento")
    private String tipoDocumento;

    @Column(name = "numero_documento")
    private String numeroDocumento;

    @Column(name = "razon_social_nombre")
    private String razonSocialNombre;

    private String direccion;
    private String telefono;
    private Boolean activo;
    private String correo;

    @Column(name = "fecha_registro")
    private LocalDateTime fechaRegistro;
}
