package com.tato.almacen.repository;

import com.tato.almacen.model.PedidoAlmacen;
import com.tato.almacen.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface PedidoAlmacenRepository extends JpaRepository<PedidoAlmacen, Long> {

    List<PedidoAlmacen> findBySucursalIdAndEstado(Long sucursalId, String estado);

    List<PedidoAlmacen> findBySucursalIdOrderByFechaCreacionDesc(Long sucursalId);

    /**
     * Auto-asignacion atomica: solo asigna si el pedido SIGUE en PENDIENTE.
     * Devuelve la cantidad de filas afectadas (0 = alguien mas se lo gano).
     *
     * IMPORTANTE: en un UPDATE de JPQL solo se puede asignar la asociacion
     * completa (p.almacenero = :almacenero), NO navegar a su campo interno
     * (p.almacenero.id = ...) -- eso no es JPQL valido y falla en runtime.
     */
    @Modifying
    @Query("UPDATE PedidoAlmacen p SET p.almacenero = :almacenero, p.estado = 'ASIGNADO', p.fechaAsignacion = :ahora " +
           "WHERE p.id = :id AND p.estado = 'PENDIENTE'")
    int asignarSiPendiente(@Param("id") Long id, @Param("almacenero") Usuario almacenero, @Param("ahora") LocalDateTime ahora);
}
