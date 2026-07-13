package com.tato.almacen.repository;

import com.tato.almacen.model.InventarioSucursal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface InventarioSucursalRepository extends JpaRepository<InventarioSucursal, Long> {

    List<InventarioSucursal> findBySucursalIdAndActivoTrue(Long sucursalId);

    Optional<InventarioSucursal> findByProductoIdAndSucursalId(Long productoId, Long sucursalId);

    @Query("SELECT COUNT(i) FROM InventarioSucursal i " +
           "WHERE i.sucursal.id = :sucursalId AND i.activo = true AND i.stock <= i.stockMinimo")
    Long countStockCritico(@Param("sucursalId") Long sucursalId);
}
