package cl.ms_reparacion.controller;

import cl.ms_reparacion.dto.ReparacionRequestDTO;
import cl.ms_reparacion.dto.ReparacionResponseDTO;
import cl.ms_reparacion.service.ReparacionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reparaciones")
public class ReparacionController {

    private final ReparacionService reparacionService;

    public ReparacionController(ReparacionService reparacionService) {
        this.reparacionService = reparacionService;
    }

    @PostMapping
    public ResponseEntity<ReparacionResponseDTO> crear(@Valid @RequestBody ReparacionRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(reparacionService.crearReparacion(dto));
    }

    @GetMapping
    public ResponseEntity<List<ReparacionResponseDTO>> listarTodas() {
        return ResponseEntity.ok(reparacionService.listarTodas());
    }

    @GetMapping("/cliente/{rut}")
    public ResponseEntity<List<ReparacionResponseDTO>> listarPorRut(@PathVariable String rut) {
        return ResponseEntity.ok(reparacionService.listarPorRut(rut));
    }

    @PatchMapping("/{id}/estado")
    public ResponseEntity<ReparacionResponseDTO> actualizarEstado(
            @PathVariable Long id,
            @RequestParam String nuevoEstado) {
        return ResponseEntity.ok(reparacionService.actualizarEstado(id, nuevoEstado));
    }
}