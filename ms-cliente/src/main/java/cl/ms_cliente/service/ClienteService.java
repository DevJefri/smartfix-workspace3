package cl.ms_cliente.service;

import java.util.List;

import cl.ms_cliente.dto.ClienteRequestDTO;
import cl.ms_cliente.dto.ClienteResponseDTO;

public interface ClienteService {
    ClienteResponseDTO guardarCliente(ClienteRequestDTO dto);
    ClienteResponseDTO obtenerPorRut(String rut);
    ClienteResponseDTO actualizarPorRut(String rut, ClienteRequestDTO dto);
    void eliminarPorRut(String rut);
    List<ClienteResponseDTO> listarClientes();
}