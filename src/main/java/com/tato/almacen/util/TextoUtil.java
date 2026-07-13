package com.tato.almacen.util;

import java.text.Normalizer;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Utilidades de texto compartidas para matching aproximado
 * (Asistente de voz, Registrar Compra anti-duplicados).
 */
public final class TextoUtil {

    private static final Pattern NO_ALFANUMERICO = Pattern.compile("[^a-z0-9\\s]");

    private TextoUtil() {}

    public static String normalizar(String texto) {
        if (texto == null) return "";
        String sinTildes = Normalizer.normalize(texto.toLowerCase(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return NO_ALFANUMERICO.matcher(sinTildes).replaceAll(" ").trim();
    }

    public static Set<String> tokenizar(String texto) {
        Set<String> tokens = new HashSet<>();
        String normalizado = normalizar(texto);
        if (normalizado.isBlank()) return tokens;
        for (String t : normalizado.split("\\s+")) {
            if (t.length() >= 3) tokens.add(t);
        }
        return tokens;
    }

    /**
     * Similitud por superposicion de tokens (Jaccard), en porcentaje 0-100.
     * Suficiente para detectar "Aceite Repsol 20w-50" vs "aceite repsol 20w50"
     * sin necesidad de un motor de similitud mas pesado.
     */
    public static double similitud(String a, String b) {
        Set<String> tokensA = tokenizar(a);
        Set<String> tokensB = tokenizar(b);
        if (tokensA.isEmpty() || tokensB.isEmpty()) return 0.0;

        Set<String> interseccion = new HashSet<>(tokensA);
        interseccion.retainAll(tokensB);

        Set<String> union = new HashSet<>(tokensA);
        union.addAll(tokensB);

        if (union.isEmpty()) return 0.0;
        return (interseccion.size() * 100.0) / union.size();
    }
}
