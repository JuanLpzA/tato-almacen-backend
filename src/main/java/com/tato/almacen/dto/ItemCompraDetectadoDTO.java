package com.tato.almacen.dto;

import java.math.BigDecimal;

/**
 * Un item detectado por Gemini en la foto (etiqueta o boleta), ya cruzado
 * contra el catalogo de productos existente para sugerir si es un producto
 * que ya existe o si hay que crearlo nuevo.
 */
public record ItemCompraDetectadoDTO(
        String nombreDetectado,
        String marcaDetectada,
        Integer cantidadDetectada,
        BigDecimal precioUnitarioDetectado, // puede venir null si no era visible en la foto
        Boolean productoExistente,
        Long productoIdSugerido,            // null si no hay match suficiente
        String nombreProductoSugerido,      // nombre real en BD, para que el usuario compare
        Double similitud                    // 0-100, que tan parecido es al producto sugerido
) {}
