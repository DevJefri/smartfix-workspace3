package cl.ms_reparacion.service;

import cl.ms_reparacion.dto.ReparacionRequestDTO;
import cl.ms_reparacion.dto.ReparacionResponseDTO;
import java.util.List;

public interface ReparacionService {
    ReparacionResponseDTO crearReparacion(ReparacionRequestDTO dto);
    List<ReparacionResponseDTO> listarPorRut(String rutCliente);
    List<ReparacionResponseDTO> listarTodas();
    ReparacionResponseDTO actualizarEstado(Long id, String nuevoEstado);
}