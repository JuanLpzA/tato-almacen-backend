package com.tato.almacen.service;

import com.tato.almacen.dto.DetallePedidoAlmacenDTO;
import com.tato.almacen.dto.PedidoAlmacenDTO;
import com.tato.almacen.dto.PedidoAlmacenDetalleDTO;
import com.tato.almacen.exception.ApiException;
import com.tato.almacen.model.*;
import com.tato.almacen.repository.*;
import com.tato.almacen.service.notificacion.NotificacionPushService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PedidoAlmacenService {

    private final PedidoAlmacenRepository pedidoAlmacenRepository;
    private final DetallePedidoAlmacenRepository detallePedidoAlmacenRepository;
    private final InventarioSucursalRepository inventarioSucursalRepository;
    private final UbicacionProductoRepository ubicacionProductoRepository;
    private final DispositivoNotificacionRepository dispositivoNotificacionRepository;
    private final SucursalRepository sucursalRepository;
    private final UsuarioRepository usuarioRepository;
    private final HistorialRepository historialRepository;
    private final NotificacionPushService notificacionPushService;

    // ==================== Creacion (simulada, para pruebas) ====================

    /**
     * Endpoint temporal para poder probar el modulo Almacen de forma
     * aislada, mientras el sistema web de gestion (que en el futuro creara
     * estos pedidos automaticamente al registrar una venta presencial) no
     * este implementado todavia. Toma 1-3 productos al azar con stock > 0
     * de la sucursal.
     */
    @Transactional
    public PedidoAlmacenDTO simular(Long sucursalId, String clienteNombre) {
        Sucursal sucursal = sucursalRepository.findById(sucursalId)
                .orElseThrow(() -> new ApiException("Sucursal no encontrada", HttpStatus.NOT_FOUND));

        List<InventarioSucursal> conStock = inventarioSucursalRepository.findBySucursalIdAndActivoTrue(sucursalId)
                .stream()
                .filter(i -> i.getStock() != null && i.getStock() > 0)
                .collect(Collectors.toList());

        if (conStock.isEmpty()) {
            throw new ApiException("No hay productos con stock en esta sucursal para simular un pedido");
        }

        Collections.shuffle(conStock);
        int cantidadItems = Math.min(conStock.size(), 1 + new Random().nextInt(3)); // 1 a 3 items
        List<InventarioSucursal> seleccionados = conStock.subList(0, cantidadItems);

        PedidoAlmacen pedido = new PedidoAlmacen();
        pedido.setSucursal(sucursal);
        pedido.setEstado("PENDIENTE");
        pedido.setClienteNombre(clienteNombre != null && !clienteNombre.isBlank() ? clienteNombre : "Cliente de prueba");
        pedido.setObservaciones("Pedido simulado para pruebas del modulo Almacen");
        pedido.setFechaCreacion(LocalDateTime.now());
        pedido = pedidoAlmacenRepository.save(pedido);

        for (InventarioSucursal inv : seleccionados) {
            int cantidad = 1 + new Random().nextInt(Math.min(3, inv.getStock()));

            String ubicacionSnapshot = ubicacionProductoRepository
                    .findByProductoIdAndSucursalId(inv.getProducto().getId(), sucursalId)
                    .map(UbicacionProducto::getAbreviatura)
                    .orElse("Sin ubicación asignada");

            DetallePedidoAlmacen detalle = new DetallePedidoAlmacen();
            detalle.setPedidoAlmacen(pedido);
            detalle.setProducto(inv.getProducto());
            detalle.setCantidad(cantidad);
            detalle.setUbicacionSnapshot(ubicacionSnapshot);
            detalle.setRecogido(false);
            detallePedidoAlmacenRepository.save(detalle);
        }

        notificarNuevoPedido(sucursal, pedido);

        return toDTO(pedido);
    }

    /**
     * Dispara la notificacion push para un pedido que YA existe en BD
     * (creado por el sistema principal al procesar una venta presencial,
     * no por el endpoint de simulacion). Se usa desde
     * AlmacenInternoController via autenticacion interna.
     */
    @Transactional
    public void notificarPedidoExistente(Long pedidoId) {
        PedidoAlmacen pedido = obtenerPedido(pedidoId);
        notificarNuevoPedido(pedido.getSucursal(), pedido);
    }

    private void notificarNuevoPedido(Sucursal sucursal, PedidoAlmacen pedido) {
        List<String> tokens = dispositivoNotificacionRepository
                .findAlmacenerosActivosPorSucursal(sucursal.getId())
                .stream()
                .map(DispositivoNotificacion::getTokenPush)
                .toList();

        notificacionPushService.enviarATokens(
                tokens,
                "Nuevo pedido para preparar",
                "Pedido #%d - %s".formatted(pedido.getId(), pedido.getClienteNombre()),
                Map.of("tipo", "PEDIDO_ALMACEN", "pedidoId", String.valueOf(pedido.getId()))
        );
    }

    // ==================== Consulta ====================

    public List<PedidoAlmacenDTO> listar(Long sucursalId, String estado) {
        List<PedidoAlmacen> pedidos = (estado != null && !estado.isBlank())
                ? pedidoAlmacenRepository.findBySucursalIdAndEstado(sucursalId, estado.toUpperCase())
                : pedidoAlmacenRepository.findBySucursalIdOrderByFechaCreacionDesc(sucursalId);

        return pedidos.stream().map(this::toDTO).toList();
    }

    public PedidoAlmacenDetalleDTO getDetalle(Long pedidoId, Long sucursalId) {
        PedidoAlmacen pedido = obtenerPedidoDeSucursal(pedidoId, sucursalId);
        List<DetallePedidoAlmacenDTO> detalles = detallePedidoAlmacenRepository.findByPedidoAlmacenId(pedidoId)
                .stream()
                .map(this::toDetalleDTO)
                .toList();
        return new PedidoAlmacenDetalleDTO(toDTO(pedido), detalles);
    }

    // ==================== Asignacion ====================

    @Transactional
    public PedidoAlmacenDTO asignarme(Long pedidoId, Long usuarioId, Long sucursalId) {
        obtenerPedidoDeSucursal(pedidoId, sucursalId); // valida que el pedido sea de esta sucursal

        // getReferenceById NO dispara un SELECT (solo crea un proxy con el ID),
        // suficiente para el UPDATE de abajo sin gastar una consulta extra.
        Usuario almacenero = usuarioRepository.getReferenceById(usuarioId);
        int filasAfectadas = pedidoAlmacenRepository.asignarSiPendiente(pedidoId, almacenero, LocalDateTime.now());

        if (filasAfectadas == 0) {
            PedidoAlmacen pedido = obtenerPedido(pedidoId);
            if ("PENDIENTE".equals(pedido.getEstado())) {
                throw new ApiException("No se pudo asignar el pedido, intenta de nuevo", HttpStatus.CONFLICT);
            }
            throw new ApiException("Este pedido ya fue tomado por otro almacenero", HttpStatus.CONFLICT);
        }

        return toDTO(obtenerPedido(pedidoId));
    }

    // ==================== Preparacion / entrega ====================

    @Transactional
    public void marcarRecogido(Long pedidoId, Long detalleId, Long sucursalId) {
        obtenerPedidoDeSucursal(pedidoId, sucursalId);

        DetallePedidoAlmacen detalle = detallePedidoAlmacenRepository.findById(detalleId)
                .filter(d -> d.getPedidoAlmacen().getId().equals(pedidoId))
                .orElseThrow(() -> new ApiException("Item no encontrado en este pedido", HttpStatus.NOT_FOUND));

        detalle.setRecogido(true);
        detallePedidoAlmacenRepository.save(detalle);
    }

    @Transactional
    public PedidoAlmacenDTO entregar(Long pedidoId, boolean forzar, Long sucursalId) {
        PedidoAlmacen pedido = obtenerPedidoDeSucursal(pedidoId, sucursalId);

        if ("ENTREGADO".equals(pedido.getEstado())) {
            throw new ApiException("Este pedido ya fue entregado");
        }

        List<DetallePedidoAlmacen> detalles = detallePedidoAlmacenRepository.findByPedidoAlmacenId(pedidoId);

        if (!forzar) {
            List<String> pendientes = detalles.stream()
                    .filter(d -> !Boolean.TRUE.equals(d.getRecogido()))
                    .map(d -> d.getProducto().getNombre())
                    .toList();
            if (!pendientes.isEmpty()) {
                throw new ApiException("Faltan items por recoger: " + String.join(", ", pendientes), HttpStatus.CONFLICT);
            }
        }

        for (DetallePedidoAlmacen detalle : detalles) {
            InventarioSucursal inventario = inventarioSucursalRepository
                    .findByProductoIdAndSucursalId(detalle.getProducto().getId(), pedido.getSucursal().getId())
                    .orElse(null);

            if (inventario != null) {
                int nuevoStock = Math.max(0, inventario.getStock() - detalle.getCantidad());
                inventario.setStock(nuevoStock);
                inventario.setUnidadesVendidas(
                        (inventario.getUnidadesVendidas() != null ? inventario.getUnidadesVendidas() : 0) + detalle.getCantidad());
                inventarioSucursalRepository.save(inventario);
            }
        }

        pedido.setEstado("ENTREGADO");
        pedido.setFechaEntrega(LocalDateTime.now());
        pedidoAlmacenRepository.save(pedido);

        Historial historial = new Historial();
        historial.setAccion("PEDIDO_ENTREGADO");
        historial.setDescripcion("Pedido de almacen #%d entregado (%d items)".formatted(pedido.getId(), detalles.size()));
        historial.setModulo("ALMACEN");
        historial.setFecha(LocalDateTime.now());
        historial.setSucursal(pedido.getSucursal());
        historial.setUsuario(pedido.getAlmacenero());
        historialRepository.save(historial);

        return toDTO(pedido);
    }

    // ==================== Helpers ====================

    private PedidoAlmacen obtenerPedido(Long id) {
        return pedidoAlmacenRepository.findById(id)
                .orElseThrow(() -> new ApiException("Pedido no encontrado", HttpStatus.NOT_FOUND));
    }

    /**
     * Igual que obtenerPedido, pero ademas valida que el pedido pertenezca
     * a la sucursal del usuario que hace la peticion (evita que un
     * almacenero de otra sucursal vea/toque pedidos ajenos adivinando el ID).
     */
    private PedidoAlmacen obtenerPedidoDeSucursal(Long id, Long sucursalId) {
        PedidoAlmacen pedido = obtenerPedido(id);
        if (!pedido.getSucursal().getId().equals(sucursalId)) {
            throw new ApiException("Este pedido no pertenece a tu sucursal", HttpStatus.FORBIDDEN);
        }
        return pedido;
    }

    private PedidoAlmacenDTO toDTO(PedidoAlmacen p) {
        int cantidadItems = detallePedidoAlmacenRepository.findByPedidoAlmacenId(p.getId()).size();
        return new PedidoAlmacenDTO(
                p.getId(),
                p.getSucursal().getId(),
                p.getEstado(),
                p.getClienteNombre(),
                p.getObservaciones(),
                p.getFechaCreacion(),
                p.getFechaAsignacion(),
                p.getFechaEntrega(),
                p.getAlmacenero() != null ? (p.getAlmacenero().getNombres() + " " + p.getAlmacenero().getApellidos()) : null,
                cantidadItems
        );
    }

    private DetallePedidoAlmacenDTO toDetalleDTO(DetallePedidoAlmacen d) {
        return new DetallePedidoAlmacenDTO(
                d.getId(),
                d.getProducto().getId(),
                d.getProducto().getNombre(),
                d.getCantidad(),
                d.getUbicacionSnapshot(),
                d.getRecogido()
        );
    }
}
