package com.tato.almacen.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
@RequiredArgsConstructor
public class JwtInterceptor implements HandlerInterceptor {

    private final JwtUtil jwtUtil;

    @Value("${internal.api.key}")
    private String internalApiKey;

    /**
     * Rutas que el sistema principal (com.tato.motorepuestos) puede llamar
     * server-a-server usando la clave interna en vez de un JWT de usuario.
     * Se limita a estas para no ampliar la superficie de ataque si la clave
     * interna se filtrara por error.
     */
    private static final String[] RUTAS_PERMITIDAS_AUTH_INTERNA = {
            "/api/admin/",
            "/api/almacen/pedidos/"
    };

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {

        if (intentarAutenticacionInterna(request)) {
            return true;
        }

        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"Token no proporcionado\"}");
            return false;
        }

        String token = authHeader.substring(7);

        if (!jwtUtil.esValido(token)) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"Token invalido o expirado\"}");
            return false;
        }

        request.setAttribute("usuarioId", jwtUtil.getUsuarioId(token));
        request.setAttribute("sucursalId", jwtUtil.getSucursalId(token));

        return true;
    }

    /**
     * Autenticacion servidor-a-servidor: el sistema principal manda
     * X-Internal-Api-Key (la clave compartida) + X-Usuario-Id / X-Sucursal-Id
     * (que el sistema principal ya conoce de su propia sesion de admin).
     * No requiere JWT porque no hay "app movil" de por medio en estas llamadas.
     */
    private boolean intentarAutenticacionInterna(HttpServletRequest request) {
        String uri = request.getRequestURI();
        boolean rutaPermitida = false;
        for (String prefijo : RUTAS_PERMITIDAS_AUTH_INTERNA) {
            if (uri.contains(prefijo)) {
                rutaPermitida = true;
                break;
            }
        }
        if (!rutaPermitida) return false;

        String claveRecibida = request.getHeader("X-Internal-Api-Key");
        if (claveRecibida == null || !claveRecibida.equals(internalApiKey)) {
            return false;
        }

        String usuarioIdHeader = request.getHeader("X-Usuario-Id");
        String sucursalIdHeader = request.getHeader("X-Sucursal-Id");

        request.setAttribute("usuarioId", usuarioIdHeader != null ? Long.parseLong(usuarioIdHeader) : 0L);
        request.setAttribute("sucursalId", sucursalIdHeader != null ? Long.parseLong(sucursalIdHeader) : 0L);

        return true;
    }
}
