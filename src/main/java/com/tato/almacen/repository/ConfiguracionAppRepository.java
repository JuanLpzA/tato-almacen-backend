package com.tato.almacen.repository;

import com.tato.almacen.model.ConfiguracionApp;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ConfiguracionAppRepository extends JpaRepository<ConfiguracionApp, Long> {
    Optional<ConfiguracionApp> findByClave(String clave);
}
