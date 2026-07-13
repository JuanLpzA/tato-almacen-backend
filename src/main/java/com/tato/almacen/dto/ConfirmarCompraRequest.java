package com.tato.almacen.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.List;

public record ConfirmarCompraRequest(
        @NotBlank String razonSocialProveedor,
        @NotBlank String rucProveedor,
        @NotBlank String tipoDocumento,      // FACTURA, BOLETA, GUIA, etc.
        @NotBlank String serie,
        @NotBlank String numeroComprobante,
        @NotBlank String condicionPago,      // CONTADO, CREDITO
        @NotBlank String moneda,             // PEN, USD
        @NotNull LocalDate fechaEmision,
        LocalDate fechaVencimiento,
        @NotNull Boolean incluyeIgv,
        String observacion,
        @NotEmpty List<@Valid ItemCompraConfirmadoRequest> items
) {}
