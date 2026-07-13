package com.tato.almacen.controller;

import com.tato.almacen.dto.FotoReferenciaDTO;
import com.tato.almacen.dto.PerfilIaResponse;
import com.tato.almacen.service.FotoReferenciaService;
import com.tato.almacen.service.PerfilIaService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/admin/productos")
@RequiredArgsConstructor
public class AdminProductoIAController {

    private final PerfilIaService perfilIaService;
    private final FotoReferenciaService fotoReferenciaService;

    /**
     * Sube 1 o mas fotos reales del producto (se guardan en Cloudinary) y
     * genera su perfil IA (descripcion visual detallada) que luego se usa
     * para comparar contra las fotos que tomen los almaceneros en
     * Identificar IA.
     *
     * Si no se envia el campo "fotos", regenera el perfil usando las fotos
     * de referencia que el producto ya tenga guardadas.
     */
    @PostMapping(value = "/{id}/generar-perfil-ia", consumes = "multipart/form-data")
    public PerfilIaResponse generarPerfil(
            @PathVariable Long id,
            @RequestParam(value = "fotos", required = false) List<MultipartFile> fotos
    ) {
        return perfilIaService.generarPerfil(id, fotos);
    }

    @PostMapping(value = "/{id}/fotos-referencia", consumes = "multipart/form-data")
    public List<FotoReferenciaDTO> subirFotos(
            @PathVariable Long id,
            @RequestParam("fotos") List<MultipartFile> fotos
    ) {
        return fotoReferenciaService.subir(id, fotos);
    }

    @GetMapping("/{id}/fotos-referencia")
    public List<FotoReferenciaDTO> listarFotos(@PathVariable Long id) {
        return fotoReferenciaService.listar(id);
    }

    @DeleteMapping("/{id}/fotos-referencia/{fotoId}")
    public void eliminarFoto(@PathVariable Long id, @PathVariable Long fotoId) {
        fotoReferenciaService.eliminar(id, fotoId);
    }
}
