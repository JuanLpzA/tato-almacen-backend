package com.tato.almacen.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;

@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private long expirationMs;

    private SecretKey getKey() {
        return Keys.hmacShaKeyFor(secret.getBytes());
    }

    public String generarToken(Long usuarioId, Long sucursalId, String correo) {
        return Jwts.builder()
                .setSubject(correo)
                .claim("usuarioId", usuarioId)
                .claim("sucursalId", sucursalId)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + expirationMs))
                .signWith(getKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    public Claims extraerClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    public boolean esValido(String token) {
        try {
            extraerClaims(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public Long getUsuarioId(String token) {
        return extraerLong(extraerClaims(token), "usuarioId");
    }

    public Long getSucursalId(String token) {
        return extraerLong(extraerClaims(token), "sucursalId");
    }

    /**
     * Extrae un claim numerico de forma segura sin depender de que el
     * proveedor JSON (Gson/Jackson) haya deserializado el numero como
     * Integer, Long o Double -- evita ClassCastException.
     */
    private Long extraerLong(Claims claims, String key) {
        Object value = claims.get(key);
        if (value == null) return null;
        if (value instanceof Number number) return number.longValue();
        return Long.valueOf(value.toString());
    }
}
