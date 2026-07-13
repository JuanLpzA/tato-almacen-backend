package com.tato.almacen.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "config_almacen")
@Getter
@Setter
public class ConfigAlmacen {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "sucursal_id")
    private Sucursal sucursal;

    @Column(name = "total_estantes")
    private Integer totalEstantes;

    @Column(name = "total_filas")
    private Integer totalFilas;

    @Column(name = "total_columnas")
    private Integer totalColumnas;
}
