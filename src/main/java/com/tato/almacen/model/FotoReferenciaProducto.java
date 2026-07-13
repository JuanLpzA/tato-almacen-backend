package com.tato.almacen.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "fotos_referencia_producto")
@Getter
@Setter
public class FotoReferenciaProducto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "producto_id")
    private Producto producto;

    @Column(name = "url_foto")
    private String urlFoto;

    @Column(name = "public_id_cloudinary")
    private String publicIdCloudinary;

    @Column(name = "es_principal")
    private Boolean esPrincipal;

    @Column(name = "fecha_subida")
    private LocalDateTime fechaSubida;
}
