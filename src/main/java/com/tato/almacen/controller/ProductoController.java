package com.tato.almacen.controller;

import com.tato.almacen.dto.UbicacionDTO;
import com.tato.almacen.service.UbicacionService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/productos")
@RequiredArgsConstructor
public class ProductoController {

    private final UbicacionService ubicacionService;

    /**
     * Ubicacion del producto en la sucursal del usuario logueado (tomada del JWT).
     * Se puede forzar otra sucursal con ?sucursalId= si algun dia hace falta
     * (ej. un admin viendo otra sucursal), pero por defecto usa la del token.
     */
    @GetMapping("/{id}/ubicacion")
    public UbicacionDTO getUbicacion(
            @PathVariable Long id,
            @RequestParam(required = false) Long sucursalId,
            HttpServletRequest request
    ) {
        Long sucursalFinal = sucursalId != null ? sucursalId : (Long) request.getAttribute("sucursalId");
        return ubicacionService.getUbicacion(id, sucursalFinal);
    }
}
