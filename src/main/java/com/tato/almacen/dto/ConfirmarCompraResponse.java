package com.tato.almacen.dto;

import java.math.BigDecimal;

public record ConfirmarCompraResponse(
        Long compraId,
        BigDecimal subtotal,
        BigDecimal igv,
        BigDecimal total,
        Integer productosNuevosCreados
) {}
