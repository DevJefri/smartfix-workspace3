package cl.ms_reparacion.repository;

import cl.ms_reparacion.model.Reparacion;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ReparacionRepository extends JpaRepository<Reparacion, Long> {
    List<Reparacion> findByRutCliente(String rutCliente);
}