package com.tato.almacen.dto;

import java.util.List;

public record AnalizarCompraResponse(
        String tipoCaptura,
        List<ItemCompraDetectadoDTO> items
) {}
