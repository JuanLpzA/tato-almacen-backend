package com.tato.almacen.dto;

import jakarta.validation.constraints.NotBlank;

public record AsistenteConsultaRequest(
        @NotBlank String texto
) {}
