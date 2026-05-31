package cl.ms_reparacion.service;

import cl.ms_reparacion.exception.ClienteNoEncontradoException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

@Service
public class ClienteVerificacionService {

    private final RestTemplate restTemplate;

    @Value("${services.cliente.url}")
    private String clienteBaseUrl;

    public ClienteVerificacionService() {
        this.restTemplate = new RestTemplate();
    }

public void verificarClienteExiste(String rut) {
        try {
            restTemplate.getForObject(clienteBaseUrl + "/api/customers/" + rut, Object.class);
        } catch (HttpClientErrorException.NotFound ex) {
            throw new ClienteNoEncontradoException(
                "No existe cliente con RUT " + rut + ". Registre al cliente primero.");
        } catch (HttpClientErrorException | ResourceAccessException ex) {
            throw new ClienteNoEncontradoException(
                "No se pudo verificar el cliente. El servicio de clientes no está disponible.");
        }
    }
}