package com.tato.almacen.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "dispositivos_notificacion")
@Getter
@Setter
public class DispositivoNotificacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;

    @ManyToOne
    @JoinColumn(name = "sucursal_id")
    private Sucursal sucursal;

    @Column(name = "token_push")
    private String tokenPush;

    private String plataforma;
    private Boolean activo;

    @Column(name = "fecha_registro")
    private LocalDateTime fechaRegistro;
}
