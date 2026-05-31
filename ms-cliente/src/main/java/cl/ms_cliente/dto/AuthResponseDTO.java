package cl.ms_cliente.dto;

public record AuthResponseDTO(
    String token,
    String username,
    String role
) {}