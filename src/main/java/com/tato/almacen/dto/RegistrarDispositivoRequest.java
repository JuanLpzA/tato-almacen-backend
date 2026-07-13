package com.tato.almacen.dto;

import jakarta.validation.constraints.NotBlank;

public record RegistrarDispositivoRequest(
        @NotBlank String tokenPush,
        String plataforma // opcional, default ANDROID
) {}
