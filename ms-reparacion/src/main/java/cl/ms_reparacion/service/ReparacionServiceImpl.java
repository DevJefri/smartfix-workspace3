package cl.ms_reparacion.service;

import cl.ms_reparacion.dto.ReparacionRequestDTO;
import cl.ms_reparacion.dto.ReparacionResponseDTO;
import cl.ms_reparacion.exception.RecursoNoEncontradoException;
import cl.ms_reparacion.model.Reparacion;
import cl.ms_reparacion.repository.ReparacionRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ReparacionServiceImpl implements ReparacionService {

    private final ReparacionRepository reparacionRepository;
    private final ClienteVerificacionService clienteVerificacionService;

    public ReparacionServiceImpl(ReparacionRepository reparacionRepository,
        ClienteVerificacionService clienteVerificacionService) {
        this.reparacionRepository = reparacionRepository;
        this.clienteVerificacionService = clienteVerificacionService;
    }

    @Override
    public ReparacionResponseDTO crearReparacion(ReparacionRequestDTO dto) {
        clienteVerificacionService.verificarClienteExiste(dto.rutCliente());

        Reparacion r = new Reparacion();
        r.setRutCliente(dto.rutCliente());
        r.setModelo(dto.modelo());
        r.setDescripcion(dto.descripcion());
        r.setEstado("RECIBIDO");
        return mapToDto(reparacionRepository.save(r));
    }

    @Override
    public List<ReparacionResponseDTO> listarPorRut(String rutCliente) {
        return reparacionRepository.findByRutCliente(rutCliente)
                .stream().map(this::mapToDto).collect(Collectors.toList());
    }

    @Override
    public List<ReparacionResponseDTO> listarTodas() {
        return reparacionRepository.findAll()
                .stream().map(this::mapToDto).collect(Collectors.toList());
    }

    @Override
    public ReparacionResponseDTO actualizarEstado(Long id, String nuevoEstado) {
        Reparacion r = reparacionRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Reparación no encontrada con id: " + id));
        r.setEstado(nuevoEstado);
        return mapToDto(reparacionRepository.save(r));
    }

    private ReparacionResponseDTO mapToDto(Reparacion r) {
        return new ReparacionResponseDTO(
            r.getId(), r.getRutCliente(), r.getModelo(), r.getDescripcion(), r.getEstado());
    }
}