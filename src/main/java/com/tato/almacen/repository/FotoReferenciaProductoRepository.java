package com.tato.almacen.repository;

import com.tato.almacen.model.FotoReferenciaProducto;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FotoReferenciaProductoRepository extends JpaRepository<FotoReferenciaProducto, Long> {
    List<FotoReferenciaProducto> findByProductoId(Long productoId);
}
