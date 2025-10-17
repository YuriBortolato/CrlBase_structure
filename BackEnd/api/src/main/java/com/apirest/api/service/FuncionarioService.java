package com.apirest.api.service;

import com.apirest.api.dto.*;
import com.apirest.api.entity.Cargo;
import com.apirest.api.entity.Funcionario;
import com.apirest.api.repository.FuncionarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class FuncionarioService {

    private final FuncionarioRepository repository;
    private final PasswordEncoder passwordEncoder;

    private static final Set<Cargo> PERMISSAO_CRIAR_FUNCIONARIO = Set.of(
            Cargo.DONO, Cargo.GERENTE, Cargo.LIDER_VENDA, Cargo.ADMIN
    );




    public FuncionarioResponseDTO criar(FuncionarioDTO dto) {

        // Validação de duplicidade
        if (repository.existsByEmail(dto.getEmail()))
            throw new RuntimeException("E-mail já cadastrado.");
        if (repository.existsByCpf(dto.getCpf()))
            throw new RuntimeException("CPF já cadastrado.");
        if (repository.existsByLogin(dto.getLogin()))
            throw new RuntimeException("Login já cadastrado.");

        // Criptografa a senha
        String senhaCriptografada = passwordEncoder.encode(dto.getSenha());

        Funcionario funcionario = Funcionario.builder()
                .cargo(dto.getCargo())
                .nomeCompleto(dto.getNomeCompleto())
                .cpf(dto.getCpf())
                .email(dto.getEmail())
                .telefone(dto.getTelefone())
                .login(dto.getLogin())
                .senhaCriptografada(senhaCriptografada)
                .build();

        Funcionario salvo = repository.save(funcionario);

        return new FuncionarioResponseDTO(
                salvo.getIdFuncionario(),
                salvo.getCargo(),
                salvo.getNomeCompleto(),
                salvo.getCpf(),
                salvo.getEmail(),
                salvo.getTelefone(),
                salvo.getLogin()
        );
    }

    public List<Funcionario> listarTodos() {
        return repository.findAll();
    }

    public Funcionario buscarPorId(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Funcionário não encontrado."));
    }
}
