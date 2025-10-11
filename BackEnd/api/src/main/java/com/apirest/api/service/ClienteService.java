package com.apirest.api.service;

import com.apirest.api.dto.ClienteDTO;
import com.apirest.api.dto.ClienteResponseDTO;
import com.apirest.api.entity.Cliente;
import com.apirest.api.repository.ClienteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ClienteService {

    private final ClienteRepository repository;
    private final PasswordEncoder passwordEncoder;

    public ClienteResponseDTO criar(ClienteDTO dto) {
        // validações de unicidade
        if (repository.existsByEmail(dto.getEmail()))
            throw new RuntimeException("Email já cadastrado.");
        if (repository.existsByCpf(dto.getCpf()))
            throw new RuntimeException("CPF já cadastrado.");
        if (repository.existsByLogin(dto.getLogin()))
            throw new RuntimeException("Login já cadastrado.");

        // criptografa senha
        String senhaHash = passwordEncoder.encode(dto.getSenha());

        Cliente cliente = Cliente.builder()
                .nomeCompleto(dto.getNomeCompleto())
                .email(dto.getEmail())
                .cpf(dto.getCpf())
                .telefone(dto.getTelefone())
                .login(dto.getLogin())
                .senhaCriptografada(senhaHash)
                .build();

        Cliente salvo = repository.save(cliente);

        return new ClienteResponseDTO(
                salvo.getIdCliente(),
                salvo.getNomeCompleto(),
                salvo.getEmail(),
                salvo.getCpf(),
                salvo.getTelefone(),
                salvo.getLogin()
        );
    }

    public List<Cliente> listarTodos() {
        return repository.findAll();
    }

    public Cliente buscarPorId(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Cliente não encontrado com ID " + id));
    }
}