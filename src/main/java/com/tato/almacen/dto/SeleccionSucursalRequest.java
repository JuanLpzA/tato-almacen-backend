package com.tato.almacen.dto;

import jakarta.validation.constraints.NotNull;

public record SeleccionSucursalRequest(
        @NotNull Long usuarioId,
        @NotNull Long sucursalId
) {}
