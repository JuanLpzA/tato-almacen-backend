package com.tato.almacen.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.tato.almacen.dto.PerfilIaResponse;
import com.tato.almacen.exception.ApiException;
import com.tato.almacen.model.PerfilIaProducto;
import com.tato.almacen.model.Producto;
import com.tato.almacen.repository.PerfilIaProductoRepository;
import com.tato.almacen.repository.ProductoRepository;
import com.tato.almacen.service.gemini.GeminiClient;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Genera (o regenera) el "perfil IA" de un producto: una descripcion visual
 * detallada que luego se usa para comparar contra fotos tomadas en Identificar IA.
 *
 * Dos formas de usarlo:
 * 1) Enviando fotos nuevas (multipart) -> se suben a Cloudinary como fotos de
 *    referencia (via FotoReferenciaService) y se analizan de una vez.
 * 2) Sin enviar fotos -> usa las fotos de referencia que el producto ya tenga
 *    guardadas en Cloudinary (para regenerar el perfil sin volver a fotografiar).
 */
@Service
@RequiredArgsConstructor
public class PerfilIaService {

    private final ProductoRepository productoRepository;
    private final PerfilIaProductoRepository perfilIaProductoRepository;
    private final FotoReferenciaService fotoReferenciaService;
    private final GeminiClient geminiClient;

    public PerfilIaResponse generarPerfil(Long productoId, List<MultipartFile> fotosNuevas) {
        Producto producto = productoRepository.findById(productoId)
                .orElseThrow(() -> new ApiException("Producto no encontrado", HttpStatus.NOT_FOUND));

        List<byte[]> imagenes;

        if (fotosNuevas != null && !fotosNuevas.isEmpty()) {
            // Se persisten en Cloudinary como fotos de referencia y se usan tal cual
            fotoReferenciaService.subir(productoId, fotosNuevas);
            imagenes = fotosNuevas.stream().map(this::leerBytes).toList();
        } else {
            // Regenerar usando las fotos que ya tiene guardadas
            imagenes = fotoReferenciaService.descargarBytes(productoId);
        }

        if (imagenes.isEmpty()) {
            throw new ApiException("Este producto no tiene fotos de referencia. Sube al menos una foto (campo 'fotos').");
        }

        String systemInstruction = """
                Eres un asistente que describe repuestos y productos de ferreteria/motorepuestos
                para un sistema de identificacion visual por foto. Analiza la(s) foto(s) del
                producto "%s" (marca: %s) y genera una descripcion visual DETALLADA y OBJETIVA:
                forma, color(es), material aparente, tipo de rosca/conexion si aplica, texto o
                codigo visible en el empaque/etiqueta, tamaño relativo aproximado.
                No inventes caracteristicas que no se vean claramente en la foto.
                Responde SOLO JSON, sin texto adicional.
                """.formatted(producto.getNombre(), producto.getMarca());

        String userContent = """
                Devuelve SOLO este JSON:
                {"descripcionIa":"descripcion detallada en español, 3-5 frases","palabrasClave":"5 a 8 palabras clave separadas por coma"}
                """;

        JsonNode datos = geminiClient.generarJson(systemInstruction, userContent, imagenes);

        PerfilIaProducto perfil = perfilIaProductoRepository.findByProductoId(productoId)
                .orElseGet(PerfilIaProducto::new);

        perfil.setProducto(producto);
        perfil.setDescripcionIa(datos.path("descripcionIa").asText(""));
        perfil.setPalabrasClave(datos.path("palabrasClave").asText(""));
        perfil.setFechaGenerado(LocalDateTime.now());

        perfilIaProductoRepository.save(perfil);

        return new PerfilIaResponse(productoId, perfil.getDescripcionIa(), perfil.getPalabrasClave());
    }

    private byte[] leerBytes(MultipartFile file) {
        try {
            return file.getBytes();
        } catch (IOException e) {
            throw new ApiException("Error leyendo la foto: " + e.getMessage());
        }
    }
}
