package com.tato.almacen.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.tato.almacen.dto.AsistenteRespuestaDTO;
import com.tato.almacen.dto.UbicacionDTO;
import com.tato.almacen.model.InventarioSucursal;
import com.tato.almacen.model.Producto;
import com.tato.almacen.model.UbicacionProducto;
import com.tato.almacen.repository.InventarioSucursalRepository;
import com.tato.almacen.repository.UbicacionProductoRepository;
import com.tato.almacen.service.gemini.GeminiClient;
import com.tato.almacen.util.TextoUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Interpreta una consulta en texto plano (ya transcrita por el
 * SpeechRecognizer nativo de Android, no hacemos voz->texto aca) y devuelve
 * el producto + ubicacion + una respuesta en español natural lista para
 * reproducir con TextToSpeech.
 *
 * Estrategia en dos pasos para ahorrar llamadas a Gemini:
 *  1. Matching directo por tokens contra nombre+marca del catalogo de la sucursal.
 *  2. Si es ambiguo o no hay match claro, se le pasa el catalogo a Gemini
 *     para que interprete la intencion y devuelva el producto mas probable.
 */
@Service
@RequiredArgsConstructor
public class AsistenteVozService {

    private final InventarioSucursalRepository inventarioSucursalRepository;
    private final UbicacionProductoRepository ubicacionProductoRepository;
    private final GeminiClient geminiClient;

    public AsistenteRespuestaDTO consultar(String textoConsulta, Long sucursalId) {
        List<InventarioSucursal> inventario = inventarioSucursalRepository.findBySucursalIdAndActivoTrue(sucursalId);

        if (inventario.isEmpty()) {
            return new AsistenteRespuestaDTO(
                    "No hay productos registrados en el inventario de esta sucursal.",
                    null, null, null, null);
        }

        Producto encontrado = buscarPorMatchingDirecto(textoConsulta, inventario);

        if (encontrado == null) {
            encontrado = buscarConGemini(textoConsulta, inventario);
        }

        if (encontrado == null) {
            return new AsistenteRespuestaDTO(
                    "No encontré ningún producto que coincida con \"" + textoConsulta + "\" en el inventario de esta sucursal.",
                    null, null, null, null);
        }

        // Variable final para poder capturarla dentro del lambda de mas abajo
        // (encontrado se reasigno arriba, no es effectively final).
        final Producto productoEncontrado = encontrado;

        InventarioSucursal inv = buscarInventario(productoEncontrado.getId(), inventario);
        Integer stock = inv != null ? inv.getStock() : null;

        Optional<UbicacionProducto> ubicacionOpt =
                ubicacionProductoRepository.findByProductoIdAndSucursalId(productoEncontrado.getId(), sucursalId);

        UbicacionDTO ubicacionDTO = ubicacionOpt.map(u ->
                new UbicacionDTO(productoEncontrado.getId(), u.getEstante(), u.getFila(), u.getColumna(), u.getAbreviatura())
        ).orElse(null);

        String texto = construirRespuesta(productoEncontrado.getNombre(), stock, ubicacionOpt.orElse(null));

        return new AsistenteRespuestaDTO(texto, productoEncontrado.getId(), productoEncontrado.getNombre(), stock, ubicacionDTO);
    }

    // ---------- Matching directo (sin IA) ----------

    private Producto buscarPorMatchingDirecto(String texto, List<InventarioSucursal> inventario) {
        Set<String> tokensConsulta = TextoUtil.tokenizar(texto);
        if (tokensConsulta.isEmpty()) return null;

        Producto mejor = null;
        int mejorScore = 0;
        int segundoScore = 0;

        for (InventarioSucursal inv : inventario) {
            Producto producto = inv.getProducto();
            Set<String> tokensProducto = TextoUtil.tokenizar(producto.getNombre() + " " + producto.getMarca());

            int score = 0;
            for (String t : tokensProducto) {
                if (tokensConsulta.contains(t)) score++;
            }

            if (score > mejorScore) {
                segundoScore = mejorScore;
                mejorScore = score;
                mejor = producto;
            } else if (score > segundoScore) {
                segundoScore = score;
            }
        }

        // Solo confiamos en el match directo si hay al menos 1 coincidencia
        // y el mejor resultado no empata con el segundo (evita ambiguedad).
        if (mejor != null && mejorScore >= 1 && mejorScore > segundoScore) {
            return mejor;
        }
        return null;
    }

    // ---------- Fallback con Gemini ----------

    private Producto buscarConGemini(String textoConsulta, List<InventarioSucursal> inventario) {
        String catalogo = inventario.stream()
                .map(inv -> "ID %d | %s | Marca: %s".formatted(
                        inv.getProducto().getId(), inv.getProducto().getNombre(), inv.getProducto().getMarca()))
                .collect(Collectors.joining("\n"));

        String systemInstruction = """
                Eres un asistente de voz para un sistema de inventario de ferreteria/motorepuestos.
                Un usuario hizo una consulta hablada (ya transcrita a texto) preguntando por un
                producto. Debes interpretar la intencion y devolver el ID del producto del catalogo
                que mas probablemente esta buscando. Si ningun producto del catalogo coincide
                razonablemente, devuelve idProducto null. Nunca inventes productos fuera del catalogo.

                CATALOGO DE LA SUCURSAL:
                """ + catalogo;

        String userContent = """
                Consulta del usuario: "%s"

                Devuelve SOLO este JSON: {"idProducto": <numero o null>}
                """.formatted(textoConsulta);

        try {
            JsonNode resultado = geminiClient.generarJson(systemInstruction, userContent, List.of());
            if (resultado.path("idProducto").isNull() || resultado.path("idProducto").isMissingNode()) {
                return null;
            }
            Long idProducto = resultado.path("idProducto").asLong();
            return inventario.stream()
                    .map(InventarioSucursal::getProducto)
                    .filter(p -> p.getId().equals(idProducto))
                    .findFirst()
                    .orElse(null);
        } catch (Exception e) {
            return null;
        }
    }

    private InventarioSucursal buscarInventario(Long productoId, List<InventarioSucursal> inventario) {
        return inventario.stream()
                .filter(i -> i.getProducto().getId().equals(productoId))
                .findFirst()
                .orElse(null);
    }

    private String construirRespuesta(String nombreProducto, Integer stock, UbicacionProducto ubicacion) {
        StringBuilder sb = new StringBuilder();
        sb.append(nombreProducto);

        if (ubicacion != null) {
            sb.append(" está en el estante ").append(ubicacion.getEstante())
              .append(", fila ").append(ubicacion.getFila())
              .append(", columna ").append(ubicacion.getColumna())
              .append(".");
        } else {
            sb.append(" no tiene una ubicación registrada todavía en el almacén.");
        }

        if (stock != null) {
            if (stock == 0) {
                sb.append(" No hay unidades disponibles en este momento.");
            } else {
                sb.append(" Hay ").append(stock).append(stock == 1 ? " unidad disponible." : " unidades disponibles.");
            }
        }

        return sb.toString();
    }
}
