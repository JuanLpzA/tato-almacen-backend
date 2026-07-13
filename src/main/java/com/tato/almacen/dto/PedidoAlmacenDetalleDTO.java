package com.tato.almacen.dto;

import java.util.List;

public record PedidoAlmacenDetalleDTO(
        PedidoAlmacenDTO pedido,
        List<DetallePedidoAlmacenDTO> detalles
) {}
