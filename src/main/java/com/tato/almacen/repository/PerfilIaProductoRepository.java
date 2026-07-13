package com.tato.almacen.repository;

import com.tato.almacen.model.PerfilIaProducto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface PerfilIaProductoRepository extends JpaRepository<PerfilIaProducto, Long> {
    Optional<PerfilIaProducto> findByProductoId(Long productoId);

    /**
     * IDs de productos que TODAVIA no tienen perfil IA generado (para el
     * panel de configuracion del sistema principal, que solo debe ofrecer
     * generar el perfil a los que les falta).
     */
    @Query("SELECT p.id FROM Producto p WHERE p.id NOT IN " +
            "(SELECT pi.producto.id FROM PerfilIaProducto pi)")
    List<Long> findProductoIdsSinPerfil();
}
