package com.tato.almacen.repository;

import com.tato.almacen.model.DispositivoNotificacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface DispositivoNotificacionRepository extends JpaRepository<DispositivoNotificacion, Long> {

    List<DispositivoNotificacion> findByUsuarioIdAndActivoTrue(Long usuarioId);

    Optional<DispositivoNotificacion> findByUsuarioIdAndTokenPush(Long usuarioId, String tokenPush);

    /**
     * Dispositivos de almaceneros activos "conectados" a una sucursal
     * (sucursal_id se actualiza cada vez que el usuario registra su token
     * despues de seleccionar sucursal en el login).
     */
    @Query("SELECT d FROM DispositivoNotificacion d " +
           "WHERE d.sucursal.id = :sucursalId AND d.activo = true AND d.usuario.rol.nombre = 'Almacenero'")
    List<DispositivoNotificacion> findAlmacenerosActivosPorSucursal(@Param("sucursalId") Long sucursalId);
}
