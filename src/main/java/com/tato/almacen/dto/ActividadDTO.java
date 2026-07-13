package com.tato.almacen.dto;

import java.time.LocalDateTime;

public record ActividadDTO(
        String accion,
        String descripcion,
        LocalDateTime fecha
) {}
