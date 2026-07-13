package com.tato.almacen.dto;

public record DetallePedidoAlmacenDTO(
        Long id,
        Long productoId,
        String nombreProducto,
        Integer cantidad,
        String ubicacionSnapshot,
        Boolean recogido
) {}
