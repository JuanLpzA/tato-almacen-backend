package com.tato.almacen.service;

import com.tato.almacen.dto.RegistrarDispositivoRequest;
import com.tato.almacen.exception.ApiException;
import com.tato.almacen.model.DispositivoNotificacion;
import com.tato.almacen.model.Sucursal;
import com.tato.almacen.model.Usuario;
import com.tato.almacen.repository.DispositivoNotificacionRepository;
import com.tato.almacen.repository.SucursalRepository;
import com.tato.almacen.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class DispositivoNotificacionService {

    private final DispositivoNotificacionRepository dispositivoNotificacionRepository;
    private final UsuarioRepository usuarioRepository;
    private final SucursalRepository sucursalRepository;

    /**
     * Registra o renueva el token FCM del dispositivo, asociandolo a la
     * sucursal actual del usuario (la del JWT). Se llama despues de cada
     * login exitoso desde la app Android.
     */
    public void registrar(Long usuarioId, Long sucursalId, RegistrarDispositivoRequest request) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new ApiException("Usuario no encontrado", HttpStatus.NOT_FOUND));
        Sucursal sucursal = sucursalRepository.findById(sucursalId)
                .orElseThrow(() -> new ApiException("Sucursal no encontrada", HttpStatus.NOT_FOUND));

        DispositivoNotificacion dispositivo = dispositivoNotificacionRepository
                .findByUsuarioIdAndTokenPush(usuarioId, request.tokenPush())
                .orElseGet(DispositivoNotificacion::new);

        dispositivo.setUsuario(usuario);
        dispositivo.setSucursal(sucursal);
        dispositivo.setTokenPush(request.tokenPush());
        dispositivo.setPlataforma(request.plataforma() != null ? request.plataforma() : "ANDROID");
        dispositivo.setActivo(true);
        if (dispositivo.getFechaRegistro() == null) {
            dispositivo.setFechaRegistro(LocalDateTime.now());
        }

        dispositivoNotificacionRepository.save(dispositivo);
    }
}
