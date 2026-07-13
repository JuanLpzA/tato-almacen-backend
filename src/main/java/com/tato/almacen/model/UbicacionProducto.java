package com.tato.almacen.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "ubicaciones_producto")
@Getter
@Setter
public class UbicacionProducto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "producto_id")
    private Producto producto;

    @ManyToOne
    @JoinColumn(name = "sucursal_id")
    private Sucursal sucursal;

    private Integer estante;
    private Integer fila;
    private Integer columna;

    public String getAbreviatura() {
        return "E" + estante + " - F" + fila + " - C" + columna;
    }
}
