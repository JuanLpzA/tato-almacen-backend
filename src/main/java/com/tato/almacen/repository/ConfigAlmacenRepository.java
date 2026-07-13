package com.tato.almacen.repository;

import com.tato.almacen.model.ConfigAlmacen;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ConfigAlmacenRepository extends JpaRepository<ConfigAlmacen, Long> {
    Optional<ConfigAlmacen> findBySucursalId(Long sucursalId);
}
