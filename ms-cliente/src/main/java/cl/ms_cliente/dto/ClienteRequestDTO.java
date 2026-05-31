package cl.ms_cliente.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ClienteRequestDTO(

    @NotBlank(message = "El RUT es obligatorio")
    @Pattern(regexp = "\\d{7,8}-[\\dkK]", message = "Formato de RUT inválido. Ejemplo: 12345678-9")
    String rut,

    @NotBlank(message = "El nombre es obligatorio")
    @Size(min = 2, max = 100, message = "El nombre debe tener entre 2 y 100 caracteres")
    String nombre,

    @Pattern(regexp = "^[+]?[0-9]{9,15}$", message = "Teléfono inválido")
    String telefono,

    @Email(message = "Email inválido")
    String email
) {}