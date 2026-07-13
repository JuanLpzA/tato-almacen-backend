package com.tato.almacen.dto;

public record UbicacionDTO(
        Long productoId,
        Integer estante,
        Integer fila,
        Integer columna,
        String abreviatura
) {}
