package com.tato.almacen.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record UbicacionUpsertRequest(
        @NotNull Long productoId,
        @NotNull Long sucursalId,
        @NotNull @Min(1) Integer estante,
        @NotNull @Min(1) Integer fila,
        @NotNull @Min(1) Integer columna
) {}
