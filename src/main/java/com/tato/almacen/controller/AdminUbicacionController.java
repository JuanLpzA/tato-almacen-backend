package com.tato.almacen.controller;

import com.tato.almacen.dto.ConfigAlmacenDTO;
import com.tato.almacen.dto.ConfigAlmacenRequest;
import com.tato.almacen.dto.ProductoSimpleDTO;
import com.tato.almacen.dto.UbicacionDTO;
import com.tato.almacen.dto.UbicacionUpsertRequest;
import com.tato.almacen.service.UbicacionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminUbicacionController {

    private final UbicacionService ubicacionService;

    /** Crea o actualiza la ubicacion E-F-C de un producto en una sucursal. */
    @PostMapping("/ubicaciones")
    public UbicacionDTO upsertUbicacion(@Valid @RequestBody UbicacionUpsertRequest request) {
        return ubicacionService.upsert(request);
    }

    @GetMapping("/config-almacen/{sucursalId}")
    public ConfigAlmacenDTO getConfig(@PathVariable Long sucursalId) {
        return ubicacionService.getConfig(sucursalId);
    }

    /** Define/actualiza el tamaño del grid (estantes x filas x columnas) de una sucursal. */
    @PutMapping("/config-almacen")
    public ConfigAlmacenDTO upsertConfig(@Valid @RequestBody ConfigAlmacenRequest request) {
        return ubicacionService.upsertConfig(request);
    }

    /** Usado por el panel del sistema principal para saber a que productos les falta asignar ubicacion. */
    @GetMapping("/productos-sin-ubicacion")
    public List<ProductoSimpleDTO> getProductosSinUbicacion(@RequestParam Long sucursalId) {
        return ubicacionService.getProductosSinUbicacion(sucursalId);
    }

    /** Usado por el panel del sistema principal para saber a que productos les falta generar perfil IA. */
    @GetMapping("/productos-sin-perfil-ia")
    public List<ProductoSimpleDTO> getProductosSinPerfilIa() {
        return ubicacionService.getProductosSinPerfilIa();
    }
}
