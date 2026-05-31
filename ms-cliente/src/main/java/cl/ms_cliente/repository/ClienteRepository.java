package cl.ms_cliente.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import cl.ms_cliente.model.Cliente;

public interface ClienteRepository extends JpaRepository<Cliente, Long> {
    Optional<Cliente> findByRut(String rut);
    boolean existsByRut(String rut);
}