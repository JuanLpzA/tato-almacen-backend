package com.tato.almacen.dto;

public record ConfigAlmacenDTO(
        Long sucursalId,
        Integer totalEstantes,
        Integer totalFilas,
        Integer totalColumnas
) {}
