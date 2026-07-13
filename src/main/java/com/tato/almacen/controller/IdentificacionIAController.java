package com.tato.almacen.controller;

import com.tato.almacen.dto.ProductoMatchDTO;
import com.tato.almacen.exception.ApiException;
import com.tato.almacen.service.IdentificacionIAService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/identificar")
@RequiredArgsConstructor
public class IdentificacionIAController {

    private final IdentificacionIAService identificacionIAService;

    @PostMapping
    public List<ProductoMatchDTO> identificar(
            @RequestParam("foto") MultipartFile foto,
            HttpServletRequest request
    ) {
        Long sucursalId = (Long) request.getAttribute("sucursalId");
        try {
            return identificacionIAService.identificar(foto.getBytes(), sucursalId);
        } catch (IOException e) {
            throw new ApiException("Error leyendo la foto enviada: " + e.getMessage());
        }
    }
}
