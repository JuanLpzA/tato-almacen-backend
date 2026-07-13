package com.tato.almacen.controller;

import com.tato.almacen.dto.AnalizarCompraResponse;
import com.tato.almacen.dto.ConfirmarCompraRequest;
import com.tato.almacen.dto.ConfirmarCompraResponse;
import com.tato.almacen.service.RegistrarCompraService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/compras")
@RequiredArgsConstructor
public class RegistrarCompraController {

    private final RegistrarCompraService registrarCompraService;

    /**
     * Solo analiza y sugiere -- NO guarda nada en BD todavia.
     * tipoCaptura: "ETIQUETA" (foto de una caja individual) o
     *              "BOLETA" (foto de un comprobante con varios items).
     */
    @PostMapping(value = "/analizar", consumes = "multipart/form-data")
    public AnalizarCompraResponse analizar(
            @RequestParam("foto") MultipartFile foto,
            @RequestParam("tipoCaptura") String tipoCaptura
    ) {
        return registrarCompraService.analizar(foto, tipoCaptura);
    }

    /**
     * Guarda la compra ya revisada por el usuario: crea productos nuevos si
     * hace falta, registra en compras/detalle_compras y actualiza el stock
     * en inventario_sucursal.
     */
    @PostMapping("/confirmar")
    public ConfirmarCompraResponse confirmar(@Valid @RequestBody ConfirmarCompraRequest request, HttpServletRequest httpRequest) {
        Long usuarioId = (Long) httpRequest.getAttribute("usuarioId");
        Long sucursalId = (Long) httpRequest.getAttribute("sucursalId");
        return registrarCompraService.confirmar(request, usuarioId, sucursalId);
    }
}
