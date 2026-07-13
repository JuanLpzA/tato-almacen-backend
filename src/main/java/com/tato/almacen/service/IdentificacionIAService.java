package com.tato.almacen.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.tato.almacen.dto.ProductoMatchDTO;
import com.tato.almacen.model.InventarioSucursal;
import com.tato.almacen.model.PerfilIaProducto;
import com.tato.almacen.model.Producto;
import com.tato.almacen.repository.InventarioSucursalRepository;
import com.tato.almacen.repository.PerfilIaProductoRepository;
import com.tato.almacen.service.gemini.GeminiClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class IdentificacionIAService {

    private final InventarioSucursalRepository inventarioSucursalRepository;
    private final PerfilIaProductoRepository perfilIaProductoRepository;
    private final GeminiClient geminiClient;

    private static final double UMBRAL_COINCIDENCIA_FUERTE = 80.0;


    public List<ProductoMatchDTO> identificar(byte[] fotoBytes, Long sucursalId) {

        System.out.println("========== IDENTIFICACION IA ==========");
        System.out.println("Sucursal recibida: " + sucursalId);
        System.out.println("Peso de imagen recibida: " + fotoBytes.length + " bytes");


        System.out.println("Consultando inventario activo de la sucursal...");

        List<InventarioSucursal> inventario =
                inventarioSucursalRepository.findBySucursalIdAndActivoTrue(sucursalId);


        System.out.println("Productos encontrados en inventario: " + inventario.size());


        if (inventario.isEmpty()) {
            System.out.println("No hay productos activos para comparar");
            System.out.println("=======================================");
            return List.of();
        }


        System.out.println("Generando catalogo para Gemini...");


        String catalogo = inventario.stream()
                .map(this::describirParaCatalogo)
                .collect(Collectors.joining("\n"));


        System.out.println("Catalogo generado:");
        System.out.println("---------------------------------------");
        System.out.println(catalogo);
        System.out.println("---------------------------------------");


        String systemInstruction = """
                Eres un identificador visual de repuestos y articulos de ferreteria/motorepuestos
                para un sistema de inventario. Recibiras UNA foto de un producto fisico y un
                catalogo de productos disponibles en la sucursal (con su descripcion visual
                cuando exista). Compara la foto contra el catalogo y devuelve el TOP 3 mas
                parecido, con un porcentaje de coincidencia de 0 a 100.

                Si ningun producto coincide fuertemente, igual devuelve los 3 mas cercanos
                posibles, con porcentajes bajos y honestos (no fuerces coincidencias altas).
                NUNCA inventes productos que no esten en el catalogo, usa solo los IDs listados.

                CATALOGO DE LA SUCURSAL:
                """ + catalogo;


        String userContent = """
                Devuelve SOLO este JSON:
                {"resultados":[{"idProducto":1,"porcentaje":98.0},{"idProducto":2,"porcentaje":54.0},{"idProducto":3,"porcentaje":30.0}]}
                Maximo 3 resultados, ordenados de mayor a menor porcentaje.
                """;


        System.out.println("Enviando imagen y catalogo a Gemini...");
        System.out.println("Longitud systemInstruction: "
                + systemInstruction.length());
        System.out.println("Longitud userContent: "
                + userContent.length());


        JsonNode respuestaGemini = geminiClient
                .generarJson(
                        systemInstruction,
                        userContent,
                        List.of(fotoBytes)
                );


        System.out.println("Respuesta completa Gemini:");
        System.out.println(respuestaGemini.toPrettyString());


        JsonNode resultados = respuestaGemini.path("resultados");


        System.out.println("Cantidad de coincidencias recibidas: "
                + resultados.size());


        List<ProductoMatchDTO> respuesta = new ArrayList<>();


        for (JsonNode nodo : resultados) {

            Long idProducto = nodo.path("idProducto").asLong();
            double porcentaje = nodo.path("porcentaje").asDouble();


            System.out.println("---------------------------------------");
            System.out.println("ID producto recibido por IA: " + idProducto);
            System.out.println("Porcentaje IA: " + porcentaje);


            inventario.stream()
                    .filter(inv ->
                            inv.getProducto().getId().equals(idProducto)
                    )
                    .findFirst()
                    .ifPresentOrElse(inv -> {

                        System.out.println(
                                "Producto encontrado: "
                                        + inv.getProducto().getNombre()
                        );

                        respuesta.add(
                                mapear(inv, porcentaje)
                        );

                    }, () -> {

                        System.out.println(
                                "Producto ID "
                                        + idProducto
                                        + " no existe en inventario"
                        );

                    });
        }


        System.out.println("Resultado final enviado al Android:");
        respuesta.forEach(r ->
                System.out.println(
                        "Producto: " + r.nombre()
                                + " | Stock: " + r.stock()
                                + " | Coincidencia: " + r.porcentaje()
                                + "%"
                                + " | Estado: " + r.estado()
                )
        );


        System.out.println("=======================================");


        return respuesta;
    }



    private String describirParaCatalogo(InventarioSucursal inv) {

        Producto producto = inv.getProducto();


        String descripcionIa =
                perfilIaProductoRepository
                        .findByProductoId(producto.getId())
                        .map(PerfilIaProducto::getDescripcionIa)
                        .filter(d -> d != null && !d.isBlank())
                        .orElse(
                                producto.getDescripcion() != null
                                        ? producto.getDescripcion()
                                        : "sin descripcion"
                        );


        return "ID %d | %s | Marca: %s | Stock: %d | Descripcion visual: %s"
                .formatted(
                        producto.getId(),
                        producto.getNombre(),
                        producto.getMarca(),
                        inv.getStock(),
                        descripcionIa
                );
    }



    private ProductoMatchDTO mapear(
            InventarioSucursal inv,
            double porcentaje
    ) {

        String estado;


        if (inv.getStock() == null || inv.getStock() == 0) {

            estado = "Sin stock";

        } else if (porcentaje >= UMBRAL_COINCIDENCIA_FUERTE) {

            estado = "Coincidencia";

        } else {

            estado = "Alternativa";
        }


        System.out.println(
                "Mapeando producto: "
                        + inv.getProducto().getNombre()
                        + " => "
                        + estado
        );


        return new ProductoMatchDTO(
                inv.getProducto().getId(),
                inv.getProducto().getNombre(),
                inv.getStock(),
                porcentaje,
                estado
        );
    }
}