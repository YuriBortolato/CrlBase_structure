package com.apirest.api.service;

import com.apirest.api.dto.LoginDTO;
import com.apirest.api.dto.LoginResponseDTO;
import com.apirest.api.entity.Cliente;
import com.apirest.api.entity.Funcionario;
import com.apirest.api.repository.ClienteRepository;
import com.apirest.api.repository.FuncionarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final FuncionarioRepository funcionarioRepository;
    private final ClienteRepository clienteRepository;
    private final PasswordEncoder passwordEncoder;

    public LoginResponseDTO autenticar(LoginDTO dto) {
        // 1. Tenta achar na tabela de Funcionários (Prioridade)
        // O findByLogin precisa retornar Optional<Funcionario> no Repository (adicione se não tiver)
        Optional<Funcionario> funcionarioOpt = funcionarioRepository.findAll().stream()
                .filter(f -> f.getLogin().equals(dto.getLogin()) && f.isAtivo())
                .findFirst();

        if (funcionarioOpt.isPresent()) {
            Funcionario f = funcionarioOpt.get();
            if (passwordEncoder.matches(dto.getSenha(), f.getSenhaCriptografada())) {
                return LoginResponseDTO.builder()
                        .id(f.getIdFuncionario())
                        .nome(f.getNomeCompleto())
                        .tipoUsuario("FUNCIONARIO")
                        .cargo(f.getCargo().name())
                        .build();
            }
        }

        // 2. Se não achou ou senha errada, tenta na tabela de Clientes
        // O findByLogin precisa retornar Optional<Cliente> no Repository
        Optional<Cliente> clienteOpt = clienteRepository.findByCpf(dto.getLogin()) // Tenta CPF
                .or(() -> clienteRepository.findAll().stream()
                        .filter(c -> c.getLogin().equals(dto.getLogin()))
                        .findFirst()); // Ou tenta Login

        if (clienteOpt.isPresent()) {
            Cliente c = clienteOpt.get();
            if (c.isAtivo() && passwordEncoder.matches(dto.getSenha(), c.getSenhaCriptografada())) {
                return LoginResponseDTO.builder()
                        .id(c.getIdCliente())
                        .nome(c.getNomeCompleto())
                        .tipoUsuario("CLIENTE")
                        .cargo("CLIENTE")
                        .build();
            }
        }

        throw new RuntimeException("Usuário ou senha inválidos");
    }
}