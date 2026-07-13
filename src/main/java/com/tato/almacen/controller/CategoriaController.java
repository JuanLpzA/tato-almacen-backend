package com.tato.almacen.controller;

import com.tato.almacen.dto.CategoriaDTO;
import com.tato.almacen.repository.CategoriaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/categorias")
@RequiredArgsConstructor
public class CategoriaController {

    private final CategoriaRepository categoriaRepository;

    /**
     * Usado por la app Android en Registrar Compra: cuando un item detectado
     * no coincide con ningun producto existente, el usuario elige la
     * categoria para crear el producto nuevo.
     */
    @GetMapping
    public List<CategoriaDTO> listar() {
        return categoriaRepository.findAll().stream()
                .map(c -> new CategoriaDTO(c.getId(), c.getNombre()))
                .toList();
    }
}
