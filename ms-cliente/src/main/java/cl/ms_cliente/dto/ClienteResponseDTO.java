package cl.ms_cliente.dto;

public record ClienteResponseDTO(
        String rut,
        String nombre,
        String telefono,
        String email
) {
}