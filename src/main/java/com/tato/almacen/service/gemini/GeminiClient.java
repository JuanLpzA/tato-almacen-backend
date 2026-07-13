package com.tato.almacen.service.gemini;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tato.almacen.exception.ApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;

/**
 * Cliente generico para llamar a Gemini pidiendo SIEMPRE una respuesta JSON.
 * Se reutiliza tanto para generar el perfil IA de un producto como para
 * identificar una foto contra el catalogo (mismo patron que en Altoque).
 */
@Component
public class GeminiClient {

    @Value("${gemini.api.key}")
    private String apiKey;

    @Value("${gemini.model}")
    private String modelo;

    private final RestClient restClient = RestClient.create();
    private final ObjectMapper mapper = new ObjectMapper();

    public JsonNode generarJson(String systemInstruction, String userText, List<byte[]> imagenes) {
        try {
            List<Object> parts = new ArrayList<>();
            parts.add(Map.of("text", userText));
            for (byte[] img : imagenes) {
                parts.add(Map.of("inline_data", Map.of(
                        "mime_type", "image/jpeg",
                        "data", Base64.getEncoder().encodeToString(img)
                )));
            }

            Map<String, Object> body = Map.of(
                    "systemInstruction", Map.of("parts", List.of(Map.of("text", systemInstruction))),
                    "contents", List.of(Map.of("parts", parts)),
                    "generationConfig", Map.of(
                            "temperature", 0.1,
                            "response_mime_type", "application/json"
                    )
            );

            String url = "https://generativelanguage.googleapis.com/v1beta/models/"
                    + modelo + ":generateContent?key=" + apiKey;

            String respuestaRaw = restClient.post()
                    .uri(url)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(String.class);

            if (respuestaRaw == null) {
                throw new ApiException("Gemini no devolvio respuesta");
            }

            JsonNode root = mapper.readTree(respuestaRaw);
            String texto = root.path("candidates").get(0)
                    .path("content").path("parts").get(0)
                    .path("text").asText().trim();

            if (texto.contains("{")) {
                texto = texto.substring(texto.indexOf('{'), texto.lastIndexOf('}') + 1);
            }

            return mapper.readTree(texto);

        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            throw new ApiException("Error al consultar Gemini: " + e.getMessage());
        }
    }
}
