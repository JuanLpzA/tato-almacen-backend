package com.tato.almacen.dto;

public record ProductoMatchDTO(
        Long idProducto,
        String nombre,
        Integer stock,
        Double porcentaje,
        String estado // "Coincidencia" | "Alternativa" | "Sin stock"
) {}
