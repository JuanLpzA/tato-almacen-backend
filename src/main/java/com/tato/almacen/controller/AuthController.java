package com.tato.almacen.controller;

import com.tato.almacen.dto.*;
import com.tato.almacen.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @PostMapping("/sucursal")
    public TokenResponse seleccionarSucursal(@Valid @RequestBody SeleccionSucursalRequest request) {
        return authService.seleccionarSucursal(request);
    }
}
