package cl.ms_reparacion.dto;

public record ReparacionResponseDTO(
    Long id,
    String rutCliente,
    String modelo,
    String descripcion,
    String estado
) {}