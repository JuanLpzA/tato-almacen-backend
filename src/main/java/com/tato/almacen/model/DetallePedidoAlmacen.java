package com.tato.almacen.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "detalle_pedido_almacen")
@Getter
@Setter
public class DetallePedidoAlmacen {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "pedido_almacen_id")
    private PedidoAlmacen pedidoAlmacen;

    @ManyToOne
    @JoinColumn(name = "producto_id")
    private Producto producto;

    private Integer cantidad;

    @Column(name = "ubicacion_snapshot")
    private String ubicacionSnapshot;

    private Boolean recogido;
}
