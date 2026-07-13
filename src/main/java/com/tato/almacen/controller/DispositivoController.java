package com.tato.almacen.controller;

import com.tato.almacen.dto.RegistrarDispositivoRequest;
import com.tato.almacen.service.DispositivoNotificacionService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dispositivos")
@RequiredArgsConstructor
public class DispositivoController {

    private final DispositivoNotificacionService dispositivoNotificacionService;

    /**
     * Se llama despues de cada login exitoso (cuando ya se selecciono
     * sucursal y se tiene el JWT) para registrar/renovar el token FCM
     * del dispositivo y asociarlo a la sucursal actual.
     */
    @PostMapping("/registrar")
    public void registrar(@Valid @RequestBody RegistrarDispositivoRequest request, HttpServletRequest httpRequest) {
        Long usuarioId = (Long) httpRequest.getAttribute("usuarioId");
        Long sucursalId = (Long) httpRequest.getAttribute("sucursalId");
        dispositivoNotificacionService.registrar(usuarioId, sucursalId, request);
    }
}
