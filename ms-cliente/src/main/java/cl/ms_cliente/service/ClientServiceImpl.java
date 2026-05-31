package cl.ms_cliente.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import cl.ms_cliente.dto.ClienteRequestDTO;
import cl.ms_cliente.dto.ClienteResponseDTO;
import cl.ms_cliente.exception.RecursoNoEncontradoException;
import cl.ms_cliente.exception.RutDuplicadoException;
import cl.ms_cliente.model.Cliente;
import cl.ms_cliente.repository.ClienteRepository;

@Service
public class ClientServiceImpl implements ClienteService {

    private final ClienteRepository clienteRepository;

    public ClientServiceImpl(ClienteRepository clienteRepository) {
        this.clienteRepository = clienteRepository;
    }

    @Override
    public ClienteResponseDTO guardarCliente(ClienteRequestDTO dto) {
        if (clienteRepository.existsByRut(dto.rut())) {
            throw new RutDuplicadoException("Ya existe un cliente con el RUT: " + dto.rut());
        }
        Cliente cliente = new Cliente();
        cliente.setRut(dto.rut());
        cliente.setNombre(dto.nombre());
        cliente.setTelefono(dto.telefono());
        cliente.setEmail(dto.email());
        return mapToDto(clienteRepository.save(cliente));
    }

    @Override
    public ClienteResponseDTO obtenerPorRut(String rut) {
        Cliente cliente = clienteRepository.findByRut(rut)
                .orElseThrow(() -> new RecursoNoEncontradoException("Cliente no encontrado con RUT: " + rut));
        return mapToDto(cliente);
    }

    @Override
    public ClienteResponseDTO actualizarPorRut(String rut, ClienteRequestDTO dto) {
        Cliente existente = clienteRepository.findByRut(rut)
                .orElseThrow(() -> new RecursoNoEncontradoException("Cliente no encontrado con RUT: " + rut));
        existente.setNombre(dto.nombre());
        existente.setTelefono(dto.telefono());
        existente.setEmail(dto.email());
        return mapToDto(clienteRepository.save(existente));
    }

    @Override
    public void eliminarPorRut(String rut) {
        Cliente cliente = clienteRepository.findByRut(rut)
                .orElseThrow(() -> new RecursoNoEncontradoException("Cliente no encontrado con RUT: " + rut));
        clienteRepository.delete(cliente);
    }

    @Override
    public List<ClienteResponseDTO> listarClientes() {
        return clienteRepository.findAll().stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    private ClienteResponseDTO mapToDto(Cliente c) {
        return new ClienteResponseDTO(c.getRut(), c.getNombre(), c.getTelefono(), c.getEmail());
    }
}