package com.tato.almacen.service;

import com.tato.almacen.dto.ConfigAlmacenDTO;
import com.tato.almacen.dto.ConfigAlmacenRequest;
import com.tato.almacen.dto.ProductoSimpleDTO;
import com.tato.almacen.dto.UbicacionDTO;
import com.tato.almacen.dto.UbicacionUpsertRequest;
import com.tato.almacen.exception.ApiException;
import com.tato.almacen.model.ConfigAlmacen;
import com.tato.almacen.model.Producto;
import com.tato.almacen.model.Sucursal;
import com.tato.almacen.model.UbicacionProducto;
import com.tato.almacen.repository.ConfigAlmacenRepository;
import com.tato.almacen.repository.PerfilIaProductoRepository;
import com.tato.almacen.repository.ProductoRepository;
import com.tato.almacen.repository.SucursalRepository;
import com.tato.almacen.repository.UbicacionProductoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UbicacionService {

    private final UbicacionProductoRepository ubicacionProductoRepository;
    private final ProductoRepository productoRepository;
    private final SucursalRepository sucursalRepository;
    private final ConfigAlmacenRepository configAlmacenRepository;
    private final PerfilIaProductoRepository perfilIaProductoRepository;

    public UbicacionDTO getUbicacion(Long productoId, Long sucursalId) {
        UbicacionProducto u = ubicacionProductoRepository.findByProductoIdAndSucursalId(productoId, sucursalId)
                .orElseThrow(() -> new ApiException("Ubicacion no registrada para este producto en esta sucursal", HttpStatus.NOT_FOUND));
        return toDTO(u);
    }

    public UbicacionDTO upsert(UbicacionUpsertRequest req) {
        Producto producto = productoRepository.findById(req.productoId())
                .orElseThrow(() -> new ApiException("Producto no encontrado", HttpStatus.NOT_FOUND));
        Sucursal sucursal = sucursalRepository.findById(req.sucursalId())
                .orElseThrow(() -> new ApiException("Sucursal no encontrada", HttpStatus.NOT_FOUND));

        validarContraGrid(sucursal.getId(), req.estante(), req.fila(), req.columna());

        UbicacionProducto u = ubicacionProductoRepository.findByProductoIdAndSucursalId(req.productoId(), req.sucursalId())
                .orElseGet(UbicacionProducto::new);

        u.setProducto(producto);
        u.setSucursal(sucursal);
        u.setEstante(req.estante());
        u.setFila(req.fila());
        u.setColumna(req.columna());

        return toDTO(ubicacionProductoRepository.save(u));
    }

    public ConfigAlmacenDTO getConfig(Long sucursalId) {
        ConfigAlmacen config = configAlmacenRepository.findBySucursalId(sucursalId)
                .orElseThrow(() -> new ApiException("Esta sucursal no tiene grid de almacen configurado", HttpStatus.NOT_FOUND));
        return toConfigDTO(config);
    }

    public ConfigAlmacenDTO upsertConfig(ConfigAlmacenRequest req) {
        Sucursal sucursal = sucursalRepository.findById(req.sucursalId())
                .orElseThrow(() -> new ApiException("Sucursal no encontrada", HttpStatus.NOT_FOUND));

        ConfigAlmacen config = configAlmacenRepository.findBySucursalId(req.sucursalId())
                .orElseGet(ConfigAlmacen::new);

        config.setSucursal(sucursal);
        config.setTotalEstantes(req.totalEstantes());
        config.setTotalFilas(req.totalFilas());
        config.setTotalColumnas(req.totalColumnas());

        return toConfigDTO(configAlmacenRepository.save(config));
    }

    /** Productos con inventario en la sucursal que aun no tienen ubicacion asignada. */
    public List<ProductoSimpleDTO> getProductosSinUbicacion(Long sucursalId) {
        List<Long> ids = ubicacionProductoRepository.findProductoIdsSinUbicacion(sucursalId);
        return productoRepository.findAllById(ids).stream()
                .map(this::toProductoSimpleDTO)
                .toList();
    }

    /** Productos que aun no tienen perfil IA generado (global, no depende de sucursal). */
    public List<ProductoSimpleDTO> getProductosSinPerfilIa() {
        List<Long> ids = perfilIaProductoRepository.findProductoIdsSinPerfil();
        return productoRepository.findAllById(ids).stream()
                .map(this::toProductoSimpleDTO)
                .toList();
    }

    private void validarContraGrid(Long sucursalId, Integer estante, Integer fila, Integer columna) {
        configAlmacenRepository.findBySucursalId(sucursalId).ifPresent(config -> {
            if (estante > config.getTotalEstantes() || fila > config.getTotalFilas() || columna > config.getTotalColumnas()) {
                throw new ApiException(
                        "La ubicacion (E%d-F%d-C%d) se sale del grid configurado para esta sucursal (max E%d-F%d-C%d)"
                                .formatted(estante, fila, columna,
                                        config.getTotalEstantes(), config.getTotalFilas(), config.getTotalColumnas()));
            }
        });
    }

    private UbicacionDTO toDTO(UbicacionProducto u) {
        return new UbicacionDTO(u.getProducto().getId(), u.getEstante(), u.getFila(), u.getColumna(), u.getAbreviatura());
    }

    private ConfigAlmacenDTO toConfigDTO(ConfigAlmacen c) {
        return new ConfigAlmacenDTO(c.getSucursal().getId(), c.getTotalEstantes(), c.getTotalFilas(), c.getTotalColumnas());
    }

    private ProductoSimpleDTO toProductoSimpleDTO(Producto p) {
        return new ProductoSimpleDTO(p.getId(), p.getCodigoInterno(), p.getNombre(), p.getMarca());
    }
}
