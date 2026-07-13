package com.tato.almacen.repository;

import com.tato.almacen.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    Optional<Usuario> findByCorreoElectronicoAndActivoTrue(String correo);
}
