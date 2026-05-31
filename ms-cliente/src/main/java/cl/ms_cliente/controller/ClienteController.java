package cl.ms_cliente.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import cl.ms_cliente.dto.ClienteRequestDTO;
import cl.ms_cliente.dto.ClienteResponseDTO;
import cl.ms_cliente.service.ClienteService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/customers")
public class ClienteController {

    private final ClienteService clienteService;

    public ClienteController(ClienteService clienteService) {
        this.clienteService = clienteService;
    }

    @PostMapping
    public ResponseEntity<ClienteResponseDTO> registrarCliente(@Valid @RequestBody ClienteRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(clienteService.guardarCliente(dto));
    }

    @GetMapping
    public ResponseEntity<?> listarClientes() {
        return ResponseEntity.ok(clienteService.listarClientes());
    }

    @GetMapping("/{rut}")
    public ResponseEntity<ClienteResponseDTO> obtenerPorRut(@PathVariable String rut) {
        return ResponseEntity.ok(clienteService.obtenerPorRut(rut));
    }

    @PutMapping("/{rut}")
    public ResponseEntity<ClienteResponseDTO> actualizarCliente(
            @PathVariable String rut,
            @Valid @RequestBody ClienteRequestDTO dto) {
        return ResponseEntity.ok(clienteService.actualizarPorRut(rut, dto));
    }

    @DeleteMapping("/{rut}")
    public ResponseEntity<Void> eliminarCliente(@PathVariable String rut) {
        clienteService.eliminarPorRut(rut);
        return ResponseEntity.noContent().build();
    }
}