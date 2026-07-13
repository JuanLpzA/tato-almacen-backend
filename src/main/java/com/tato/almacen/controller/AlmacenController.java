package com.tato.almacen.controller;

import com.tato.almacen.dto.PedidoAlmacenDTO;
import com.tato.almacen.dto.PedidoAlmacenDetalleDTO;
import com.tato.almacen.service.PedidoAlmacenService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/almacen")
@RequiredArgsConstructor
public class AlmacenController {

    private final PedidoAlmacenService pedidoAlmacenService;

    @GetMapping("/pedidos")
    public List<PedidoAlmacenDTO> listar(
            @RequestParam(required = false) String estado,
            HttpServletRequest request
    ) {
        Long sucursalId = (Long) request.getAttribute("sucursalId");
        return pedidoAlmacenService.listar(sucursalId, estado);
    }

    @GetMapping("/pedidos/{id}")
    public PedidoAlmacenDetalleDTO getDetalle(@PathVariable Long id, HttpServletRequest request) {
        Long sucursalId = (Long) request.getAttribute("sucursalId");
        return pedidoAlmacenService.getDetalle(id, sucursalId);
    }

    @PostMapping("/pedidos/{id}/asignarme")
    public PedidoAlmacenDTO asignarme(@PathVariable Long id, HttpServletRequest request) {
        Long usuarioId = (Long) request.getAttribute("usuarioId");
        Long sucursalId = (Long) request.getAttribute("sucursalId");
        return pedidoAlmacenService.asignarme(id, usuarioId, sucursalId);
    }

    @PostMapping("/pedidos/{id}/detalle/{detalleId}/marcar-recogido")
    public void marcarRecogido(@PathVariable Long id, @PathVariable Long detalleId, HttpServletRequest request) {
        Long sucursalId = (Long) request.getAttribute("sucursalId");
        pedidoAlmacenService.marcarRecogido(id, detalleId, sucursalId);
    }

    /**
     * ?forzar=true permite entregar aunque falten items por marcar como
     * recogidos (por si el almacenero olvido marcar alguno pero ya
     * fisicamente entrego todo). Por defecto exige que todo este recogido.
     */
    @PostMapping("/pedidos/{id}/entregar")
    public PedidoAlmacenDTO entregar(
            @PathVariable Long id,
            @RequestParam(defaultValue = "false") boolean forzar,
            HttpServletRequest request
    ) {
        Long sucursalId = (Long) request.getAttribute("sucursalId");
        return pedidoAlmacenService.entregar(id, forzar, sucursalId);
    }
}
