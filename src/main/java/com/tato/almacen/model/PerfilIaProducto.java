package com.tato.almacen.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "perfiles_ia_producto")
@Getter
@Setter
public class PerfilIaProducto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "producto_id")
    private Producto producto;

    @Lob
    @Column(name = "descripcion_ia")
    private String descripcionIa;

    @Column(name = "palabras_clave")
    private String palabrasClave;

    @Column(name = "fecha_generado")
    private LocalDateTime fechaGenerado;
}
