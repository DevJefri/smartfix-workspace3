package cl.ms_reparacion.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record ReparacionRequestDTO(

    @NotBlank(message = "El RUT del cliente es obligatorio")
    @Pattern(regexp = "\\d{7,8}-[\\dkK]", message = "Formato de RUT inválido")
    String rutCliente,

    @NotBlank(message = "El modelo es obligatorio")
    String modelo,

    @NotBlank(message = "La descripción es obligatoria")
    String descripcion
) {}