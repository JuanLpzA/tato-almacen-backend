package com.tato.almacen.service;

import com.tato.almacen.dto.*;
import com.tato.almacen.exception.ApiException;
import com.tato.almacen.model.Sucursal;
import com.tato.almacen.model.Usuario;
import com.tato.almacen.repository.SucursalRepository;
import com.tato.almacen.repository.UsuarioRepository;
import com.tato.almacen.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final SucursalRepository sucursalRepository;
    private final JwtUtil jwtUtil;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    public LoginResponse login(LoginRequest request) {

        System.out.println("========== INTENTO DE LOGIN ==========");
        System.out.println("Correo recibido: " + request.correo());
        System.out.println("Password recibido: " + request.password());

        Usuario usuario = usuarioRepository.findByCorreoElectronicoAndActivoTrue(request.correo())
                .orElseThrow(() -> {
                    System.out.println("Usuario no encontrado o inactivo");
                    return new ApiException(
                            "Correo o contraseña incorrectos",
                            HttpStatus.UNAUTHORIZED
                    );
                });

        System.out.println("Usuario encontrado: " + usuario.getNombres()
                + " " + usuario.getApellidos());

        System.out.println("Hash guardado BD: " + usuario.getPassword());

        boolean passwordCorrecta = usuario.getPassword() != null
                && !usuario.getPassword().isBlank()
                && encoder.matches(request.password(), usuario.getPassword());

        System.out.println("Password correcta: " + passwordCorrecta);

        if (!passwordCorrecta) {
            System.out.println("Contraseña incorrecta");
            throw new ApiException(
                    "Correo o contraseña incorrectos",
                    HttpStatus.UNAUTHORIZED
            );
        }

        List<SucursalSimpleDTO> sucursales = sucursalRepository.findByActivoTrue()
                .stream()
                .map(s -> new SucursalSimpleDTO(
                        s.getId(),
                        s.getNombre()
                ))
                .toList();

        System.out.println("Sucursales encontradas: " + sucursales.size());

        if (sucursales.isEmpty()) {
            throw new ApiException(
                    "No hay sucursales activas configuradas",
                    HttpStatus.CONFLICT
            );
        }

        System.out.println("Login exitoso para: " + usuario.getCorreoElectronico());
        System.out.println("======================================");

        return new LoginResponse(
                usuario.getId(),
                usuario.getNombres(),
                usuario.getApellidos(),
                sucursales
        );
    }


    public TokenResponse seleccionarSucursal(SeleccionSucursalRequest request) {

        Usuario usuario = usuarioRepository.findById(request.usuarioId())
                .orElseThrow(() -> new ApiException(
                        "Usuario no encontrado",
                        HttpStatus.NOT_FOUND
                ));

        Sucursal sucursal = sucursalRepository.findById(request.sucursalId())
                .orElseThrow(() -> new ApiException(
                        "Sucursal no encontrada",
                        HttpStatus.NOT_FOUND
                ));

        String token = jwtUtil.generarToken(
                usuario.getId(),
                sucursal.getId(),
                usuario.getCorreoElectronico()
        );

        String rolNombre = usuario.getRol() != null
                ? usuario.getRol().getNombre()
                : null;

        return new TokenResponse(
                token,
                usuario.getNombres(),
                usuario.getApellidos(),
                sucursal.getNombre(),
                rolNombre
        );
    }
}

