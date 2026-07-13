package com.tato.almacen.repository;

import com.tato.almacen.model.Venta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;

public interface VentaRepository extends JpaRepository<Venta, Long> {

    @Query("SELECT COALESCE(SUM(v.total), 0) FROM Venta v " +
           "WHERE v.sucursal.id = :sucursalId AND FUNCTION('DATE', v.fecha) = :fecha")
    BigDecimal sumVentasPorFecha(@Param("sucursalId") Long sucursalId, @Param("fecha") LocalDate fecha);
}
