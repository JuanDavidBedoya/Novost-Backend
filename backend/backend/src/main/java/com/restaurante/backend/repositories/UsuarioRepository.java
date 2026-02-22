package com.restaurante.backend.repositories;

import org.springframework.data.jpa.repository.JpaRepository;

import com.restaurante.backend.entities.Usuario;

public interface UsuarioRepository extends JpaRepository<Usuario, String> {
    
}