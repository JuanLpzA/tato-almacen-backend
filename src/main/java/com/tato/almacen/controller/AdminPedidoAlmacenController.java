package com.tato.almacen.controller;

import com.tato.almacen.dto.PedidoAlmacenDTO;
import com.tato.almacen.service.PedidoAlmacenService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/pedidos-almacen")
@RequiredArgsConstructor
public class AdminPedidoAlmacenController {

    private final PedidoAlmacenService pedidoAlmacenService;

    /**
     * Endpoint TEMPORAL para probar el modulo Almacen de forma aislada,
     * mientras el sistema web de gestion (que en el futuro creara estos
     * pedidos automaticamente al registrar una venta presencial) no este
     * implementado todavia. Crea un pedido PENDIENTE con 1-3 productos al
     * azar de la sucursal del usuario logueado.
     */
    @PostMapping("/simular")
    public PedidoAlmacenDTO simular(
            @RequestParam(required = false) String clienteNombre,
            HttpServletRequest request
    ) {
        Long sucursalId = (Long) request.getAttribute("sucursalId");
        return pedidoAlmacenService.simular(sucursalId, clienteNombre);
    }
}
