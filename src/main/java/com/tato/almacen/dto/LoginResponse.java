package com.tato.almacen.dto;

import java.util.List;

public record LoginResponse(
        Long usuarioId,
        String nombres,
        String apellidos,
        List<SucursalSimpleDTO> sucursales
) {}
