package com.tato.almacen.dto;

import java.time.LocalDateTime;

public record PedidoAlmacenDTO(
        Long id,
        Long sucursalId,
        String estado,
        String clienteNombre,
        String observaciones,
        LocalDateTime fechaCreacion,
        LocalDateTime fechaAsignacion,
        LocalDateTime fechaEntrega,
        String almaceneroNombre, // null si aun no fue asignado
        Integer cantidadItems
) {}
