package com.tato.almacen.dto;

public record AsistenteRespuestaDTO(
        String textoRespuesta,
        Long productoId,
        String nombreProducto,
        Integer stock,
        UbicacionDTO ubicacion // null si no hay ubicacion registrada o no se encontro producto
) {}
