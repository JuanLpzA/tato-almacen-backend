package com.tato.almacen.repository;

import com.tato.almacen.model.DetallePedidoAlmacen;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DetallePedidoAlmacenRepository extends JpaRepository<DetallePedidoAlmacen, Long> {
    List<DetallePedidoAlmacen> findByPedidoAlmacenId(Long pedidoAlmacenId);
}
