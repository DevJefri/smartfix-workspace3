package cl.ms_cliente.dto;

import jakarta.validation.constraints.NotBlank;

public record AuthRequestDTO(
    @NotBlank String username,
    @NotBlank String password
) {}