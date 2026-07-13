package com.tato.almacen.controller;

import com.tato.almacen.service.PedidoAlmacenService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoint interno: el sistema principal (com.tato.motorepuestos) llama
 * aca DESPUES de insertar el pedido_almacen + detalle_pedido_almacen
 * directamente en la BD (comparten la misma base de datos), solo para
 * disparar la notificacion push -- eso si depende de Firebase, que
 * unicamente vive en este backend.
 *
 * Protegido por autenticacion interna (X-Internal-Api-Key), ver JwtInterceptor.
 */
@RestController
@RequestMapping("/api/almacen/pedidos")
@RequiredArgsConstructor
public class AlmacenInternoController {

    private final PedidoAlmacenService pedidoAlmacenService;

    @PostMapping("/{id}/notificar")
    public void notificar(@PathVariable Long id) {
        pedidoAlmacenService.notificarPedidoExistente(id);
    }
}
