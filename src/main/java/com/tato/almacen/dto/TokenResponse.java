package com.tato.almacen.dto;

public record TokenResponse(
        String token,
        String nombres,
        String apellidos,
        String sucursalNombre,
        String rol
) {}
