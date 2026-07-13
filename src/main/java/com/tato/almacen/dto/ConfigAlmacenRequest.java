package com.tato.almacen.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record ConfigAlmacenRequest(
        @NotNull Long sucursalId,
        @NotNull @Min(1) Integer totalEstantes,
        @NotNull @Min(1) Integer totalFilas,
        @NotNull @Min(1) Integer totalColumnas
) {}
