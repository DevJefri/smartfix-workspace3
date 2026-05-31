package cl.ms_cliente.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import cl.ms_cliente.dto.AuthRequestDTO;
import cl.ms_cliente.dto.AuthResponseDTO;
import cl.ms_cliente.exception.RecursoNoEncontradoException;
import cl.ms_cliente.exception.RutDuplicadoException;
import cl.ms_cliente.model.Usuario;
import cl.ms_cliente.repository.UsuarioRepository;
import cl.ms_cliente.security.JwtUtil;


@Service
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public AuthService(UsuarioRepository usuarioRepository,
                       PasswordEncoder passwordEncoder,
                       JwtUtil jwtUtil) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    public AuthResponseDTO register(AuthRequestDTO dto) {
        if (usuarioRepository.existsByUsername(dto.username())) {
            throw new RutDuplicadoException("El usuario '" + dto.username() + "' ya existe");
        }
        Usuario usuario = new Usuario();
        usuario.setUsername(dto.username());
        usuario.setPassword(passwordEncoder.encode(dto.password()));
        usuario.setRole("USER");
        usuarioRepository.save(usuario);
        String token = jwtUtil.generarToken(usuario.getUsername(), usuario.getRole());
        return new AuthResponseDTO(token, usuario.getUsername(), usuario.getRole());
    }

    public AuthResponseDTO login(AuthRequestDTO dto) {
        Usuario usuario = usuarioRepository.findByUsername(dto.username())
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado"));
        if (!passwordEncoder.matches(dto.password(), usuario.getPassword())) {
            throw new RecursoNoEncontradoException("Credenciales incorrectas");
        }
        String token = jwtUtil.generarToken(usuario.getUsername(), usuario.getRole());
        return new AuthResponseDTO(token, usuario.getUsername(), usuario.getRole());
    }
}