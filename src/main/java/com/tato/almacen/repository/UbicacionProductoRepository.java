package com.tato.almacen.repository;

import com.tato.almacen.model.UbicacionProducto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UbicacionProductoRepository extends JpaRepository<UbicacionProducto, Long> {
    Optional<UbicacionProducto> findByProductoIdAndSucursalId(Long productoId, Long sucursalId);

    /**
     * Productos con inventario activo en la sucursal que TODAVIA no tienen
     * ubicacion asignada (para el panel de configuracion del sistema principal).
     */
    @Query("SELECT i.producto.id FROM InventarioSucursal i " +
            "WHERE i.sucursal.id = :sucursalId AND i.activo = true " +
            "AND i.producto.id NOT IN (" +
            "  SELECT u.producto.id FROM UbicacionProducto u WHERE u.sucursal.id = :sucursalId" +
            ")")
    List<Long> findProductoIdsSinUbicacion(@Param("sucursalId") Long sucursalId);
}
