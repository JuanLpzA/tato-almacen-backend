package com.tato.almacen.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/**
 * Item ya revisado/editado por el usuario en la app, listo para guardar.
 */
public record ItemCompraConfirmadoRequest(
        @NotNull Boolean esNuevo,
        Long productoId,          // requerido si esNuevo = false
        String nombre,            // requerido si esNuevo = true
        String marca,             // requerido si esNuevo = true
        Long categoriaId,         // requerido si esNuevo = true
        @NotNull @Min(1) Integer cantidad,
        @NotNull @DecimalMin("0.0") BigDecimal precioUnitario,
        BigDecimal precioVentaSugerido // opcional, solo aplica si esNuevo = true o si se crea inventario nuevo
) {}
