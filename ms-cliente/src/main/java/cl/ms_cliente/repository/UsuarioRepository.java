package cl.ms_cliente.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import cl.ms_cliente.model.Usuario;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    Optional<Usuario> findByUsername(String username);
    boolean existsByUsername(String username);
}