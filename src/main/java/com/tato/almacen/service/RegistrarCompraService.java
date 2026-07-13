package com.tato.almacen.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.tato.almacen.dto.*;
import com.tato.almacen.exception.ApiException;
import com.tato.almacen.model.*;
import com.tato.almacen.repository.*;
import com.tato.almacen.service.gemini.GeminiClient;
import com.tato.almacen.util.TextoUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * "Registrar Compra" con IA. Usa las tablas EXISTENTES compras/detalle_compras
 * (no se crean tablas nuevas para esto). Dos pasos:
 *   1) analizar()  -> solo lectura/sugerencia, NO guarda nada en BD.
 *   2) confirmar()  -> escritura real: crea productos nuevos si hace falta,
 *      guarda la compra, actualiza inventario_sucursal y registra historial.
 */
@Service
@RequiredArgsConstructor
public class RegistrarCompraService {

    private static final double UMBRAL_MATCH_PRODUCTO = 55.0;
    private static final BigDecimal IGV_PERU = new BigDecimal("0.18");
    private static final BigDecimal MARGEN_VENTA_DEFECTO = new BigDecimal("1.30");
    private static final int STOCK_MINIMO_DEFECTO = 5;

    private final ProductoRepository productoRepository;
    private final CategoriaRepository categoriaRepository;
    private final CompraRepository compraRepository;
    private final DetalleCompraRepository detalleCompraRepository;
    private final InventarioSucursalRepository inventarioSucursalRepository;
    private final HistorialRepository historialRepository;
    private final SucursalRepository sucursalRepository;
    private final UsuarioRepository usuarioRepository;
    private final GeminiClient geminiClient;

    // ==================== ANALIZAR (solo lectura) ====================

    public AnalizarCompraResponse analizar(MultipartFile foto, String tipoCaptura) {
        String tipo = tipoCaptura == null ? "" : tipoCaptura.trim().toUpperCase();
        if (!tipo.equals("ETIQUETA") && !tipo.equals("BOLETA")) {
            throw new ApiException("tipoCaptura debe ser ETIQUETA o BOLETA");
        }

        byte[] bytes = leerBytes(foto);
        List<Producto> catalogoCompleto = productoRepository.findAll();

        String systemInstruction = construirSystemInstruction(tipo);
        String userContent = """
                Devuelve SOLO este JSON:
                {"items":[{"nombre":"texto","marca":"texto o vacio si no se ve","cantidad":1,"precioUnitario":12.5}]}
                Si el precio unitario no es visible, usa null en precioUnitario.
                Si la cantidad no es visible, asume 1.
                """;

        JsonNode resultado = geminiClient.generarJson(systemInstruction, userContent, List.of(bytes));
        JsonNode itemsNode = resultado.path("items");

        if (!itemsNode.isArray() || itemsNode.isEmpty()) {
            throw new ApiException("No se detectó ningún producto en la imagen. Intenta con una foto más clara.");
        }

        List<ItemCompraDetectadoDTO> items = new ArrayList<>();
        for (JsonNode nodo : itemsNode) {
            String nombreDetectado = nodo.path("nombre").asText("");
            String marcaDetectada = nodo.path("marca").asText("");
            int cantidadDetectada = nodo.path("cantidad").asInt(1);
            BigDecimal precioDetectado = nodo.path("precioUnitario").isNumber()
                    ? BigDecimal.valueOf(nodo.path("precioUnitario").asDouble())
                    : null;

            items.add(buscarCoincidencia(nombreDetectado, marcaDetectada, cantidadDetectada, precioDetectado, catalogoCompleto));
        }

        return new AnalizarCompraResponse(tipo, items);
    }

    private String construirSystemInstruction(String tipo) {
        if (tipo.equals("ETIQUETA")) {
            return """
                    Eres un asistente que lee etiquetas de cajas/empaques de repuestos y
                    productos de ferreteria/motorepuestos. La foto muestra la etiqueta de
                    UNA caja individual. Extrae: nombre del producto, marca (si es visible),
                    y cantidad de unidades dentro de la caja (si esta indicado en la etiqueta,
                    si no, asume 1). El precio normalmente no aparece en una etiqueta de caja,
                    en ese caso usa null. No inventes datos que no esten visibles.
                    Responde SOLO JSON.
                    """;
        }
        return """
                Eres un asistente que lee comprobantes de compra (facturas/boletas) de un
                proveedor de repuestos y ferreteria. La foto muestra un comprobante con una
                tabla de varios productos. Extrae CADA fila de la tabla: nombre del producto
                tal como aparece, marca si se puede inferir del nombre, cantidad comprada,
                y precio unitario (si la columna existe). No inventes filas que no esten en
                la tabla, no sumes ni combines items distintos.
                Responde SOLO JSON.
                """;
    }

    private ItemCompraDetectadoDTO buscarCoincidencia(String nombreDetectado, String marcaDetectada,
                                                        int cantidad, BigDecimal precio, List<Producto> catalogo) {
        Producto mejor = null;
        double mejorScore = 0;

        String textoConsulta = nombreDetectado + " " + marcaDetectada;
        for (Producto p : catalogo) {
            double score = TextoUtil.similitud(textoConsulta, p.getNombre() + " " + p.getMarca());
            if (score > mejorScore) {
                mejorScore = score;
                mejor = p;
            }
        }

        boolean existente = mejor != null && mejorScore >= UMBRAL_MATCH_PRODUCTO;

        return new ItemCompraDetectadoDTO(
                nombreDetectado,
                marcaDetectada,
                cantidad,
                precio,
                existente,
                existente ? mejor.getId() : null,
                existente ? mejor.getNombre() : null,
                mejor != null ? Math.round(mejorScore * 10) / 10.0 : 0.0
        );
    }

    // ==================== CONFIRMAR (escritura real) ====================

    @Transactional
    public ConfirmarCompraResponse confirmar(ConfirmarCompraRequest request, Long usuarioId, Long sucursalId) {
        Sucursal sucursal = sucursalRepository.findById(sucursalId)
                .orElseThrow(() -> new ApiException("Sucursal no encontrada", HttpStatus.NOT_FOUND));
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new ApiException("Usuario no encontrado", HttpStatus.NOT_FOUND));

        BigDecimal subtotalTotal = BigDecimal.ZERO;
        int productosNuevos = 0;

        // Primero validamos y calculamos el subtotal antes de crear nada
        for (ItemCompraConfirmadoRequest item : request.items()) {
            validarItem(item);
            subtotalTotal = subtotalTotal.add(item.precioUnitario().multiply(BigDecimal.valueOf(item.cantidad())));
        }

        BigDecimal subtotal;
        BigDecimal igv;
        BigDecimal total;

        if (Boolean.TRUE.equals(request.incluyeIgv())) {
            total = subtotalTotal;
            subtotal = total.divide(BigDecimal.ONE.add(IGV_PERU), 2, RoundingMode.HALF_UP);
            igv = total.subtract(subtotal);
        } else {
            subtotal = subtotalTotal;
            igv = subtotal.multiply(IGV_PERU).setScale(2, RoundingMode.HALF_UP);
            total = subtotal.add(igv);
        }

        Compra compra = new Compra();
        compra.setCondicionPago(request.condicionPago());
        compra.setFechaEmision(request.fechaEmision());
        compra.setFechaVencimiento(request.fechaVencimiento());
        compra.setIgv(igv);
        compra.setIncluyeIgv(request.incluyeIgv());
        compra.setMoneda(request.moneda());
        compra.setNumeroComprobante(request.numeroComprobante());
        compra.setObservacion(request.observacion());
        compra.setRazonSocialProveedor(request.razonSocialProveedor());
        compra.setRucProveedor(request.rucProveedor());
        compra.setSerie(request.serie());
        compra.setSubtotal(subtotal);
        compra.setTipoDocumento(request.tipoDocumento());
        compra.setTotal(total);
        compra.setSucursal(sucursal);
        compra.setUsuario(usuario);
        compra = compraRepository.save(compra);

        for (ItemCompraConfirmadoRequest item : request.items()) {
            Producto producto;

            if (Boolean.TRUE.equals(item.esNuevo())) {
                producto = crearProductoNuevo(item);
                productosNuevos++;
            } else {
                producto = productoRepository.findById(item.productoId())
                        .orElseThrow(() -> new ApiException("Producto no encontrado: " + item.productoId(), HttpStatus.NOT_FOUND));
            }

            DetalleCompra detalle = new DetalleCompra();
            detalle.setCompra(compra);
            detalle.setProducto(producto);
            detalle.setCantidad(item.cantidad());
            detalle.setPrecioUnitario(item.precioUnitario());
            detalle.setSubtotal(item.precioUnitario().multiply(BigDecimal.valueOf(item.cantidad())));
            detalleCompraRepository.save(detalle);

            actualizarInventario(producto, sucursal, item);
        }

        Historial historial = new Historial();
        historial.setAccion("COMPRA_REGISTRADA");
        historial.setDescripcion("Compra registrada via app movil: %d producto(s), total %s %s"
                .formatted(request.items().size(), request.moneda(), total));
        historial.setModulo("REGISTRAR_COMPRA");
        historial.setFecha(LocalDateTime.now());
        historial.setSucursal(sucursal);
        historial.setUsuario(usuario);
        historialRepository.save(historial);

        return new ConfirmarCompraResponse(compra.getId(), subtotal, igv, total, productosNuevos);
    }

    private void validarItem(ItemCompraConfirmadoRequest item) {
        if (Boolean.TRUE.equals(item.esNuevo())) {
            if (item.nombre() == null || item.nombre().isBlank()) {
                throw new ApiException("Falta el nombre para un producto nuevo");
            }
            if (item.categoriaId() == null) {
                throw new ApiException("Falta la categoria para el producto nuevo: " + item.nombre());
            }
        } else if (item.productoId() == null) {
            throw new ApiException("Falta el productoId para un item existente");
        }
    }

    private Producto crearProductoNuevo(ItemCompraConfirmadoRequest item) {
        Categoria categoria = categoriaRepository.findById(item.categoriaId())
                .orElseThrow(() -> new ApiException("Categoria no encontrada: " + item.categoriaId(), HttpStatus.NOT_FOUND));

        Producto producto = new Producto();
        producto.setNombre(item.nombre());
        producto.setMarca(item.marca() != null && !item.marca().isBlank() ? item.marca() : "Sin marca");
        producto.setCategoria(categoria);
        producto.setCodigoInterno(generarCodigoInterno());
        producto.setDescripcion(null);
        producto.setImagen(null);

        return productoRepository.save(producto);
    }

    private String generarCodigoInterno() {
        // Codigo unico independiente de la numeracion manual existente (0001, 0002...)
        // para no arriesgar colisiones con codigos ya usados por el sistema web.
        return "AUTO-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private void actualizarInventario(Producto producto, Sucursal sucursal, ItemCompraConfirmadoRequest item) {
        InventarioSucursal inventario = inventarioSucursalRepository
                .findByProductoIdAndSucursalId(producto.getId(), sucursal.getId())
                .orElse(null);

        if (inventario == null) {
            inventario = new InventarioSucursal();
            inventario.setProducto(producto);
            inventario.setSucursal(sucursal);
            inventario.setStock(0);
            inventario.setStockMinimo(STOCK_MINIMO_DEFECTO);
            inventario.setUnidadesVendidas(0);
            inventario.setActivo(true);
            inventario.setPrecioCompra(item.precioUnitario());

            BigDecimal precioVenta = item.precioVentaSugerido() != null
                    ? item.precioVentaSugerido()
                    : item.precioUnitario().multiply(MARGEN_VENTA_DEFECTO).setScale(2, RoundingMode.HALF_UP);
            inventario.setPrecioVenta(precioVenta);
        } else {
            // El precio de compra se actualiza al mas reciente; el precio de venta
            // NO se toca automaticamente si ya existia (evita descuadrar precios
            // que el sistema web ya tiene definidos).
            inventario.setPrecioCompra(item.precioUnitario());
        }

        inventario.setStock(inventario.getStock() + item.cantidad());
        inventarioSucursalRepository.save(inventario);
    }

    private byte[] leerBytes(MultipartFile file) {
        try {
            return file.getBytes();
        } catch (IOException e) {
            throw new ApiException("Error leyendo la foto: " + e.getMessage());
        }
    }
}
