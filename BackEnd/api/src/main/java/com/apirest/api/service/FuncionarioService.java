package com.apirest.api.service;

import com.apirest.api.dto.*;
import com.apirest.api.entity.Cargo;
import com.apirest.api.entity.Funcionario;
import com.apirest.api.repository.FuncionarioRepository;
import jakarta.transaction.Transactional;
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

    private String limparCPF(String cpf) {
        if (cpf == null) {
            return null;
        }
        return cpf.replaceAll("[^\\d]", ""); // Remove tudo que não for dígito
    }

    public FuncionarioResponseDTO criar(FuncionarioDTO dto) {

        String cpfLimpo = limparCPF(dto.getCpf());

        // Validação de duplicidade
        if (repository.existsByEmail(dto.getEmail()))
            throw new RuntimeException("E-mail já cadastrado.");
        if (repository.existsByCpf(cpfLimpo))
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
                salvo.getLogin(),
                salvo.isAtivo()
        );
    }

    public List<Funcionario> listarTodos() {
        return repository.findAll();
    }

    public Funcionario buscarPorId(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Funcionário não encontrado."));
    }
    @Transactional
    public FuncionarioResponseDTO atualizar(Long id, FuncionarioDTO dto) {
        Funcionario funcionarioExistente = buscarPorId(id);

        boolean emailMudou = false;
        if (dto.getEmail() != null) {
            if (funcionarioExistente.getEmail() == null) {
                emailMudou = true;
            } else {
                emailMudou = !dto.getEmail().equalsIgnoreCase(funcionarioExistente.getEmail());
            }
        }
        if (emailMudou && repository.existsByEmail(dto.getEmail())) {
            throw new RuntimeException("E-mail já cadastrado em outra conta.");
        }

        boolean loginMudou = false;
        if (dto.getLogin() != null) {
            if (funcionarioExistente.getLogin() == null) {
                loginMudou = true;
            } else {
                loginMudou = !dto.getLogin().equals(funcionarioExistente.getLogin());
            }
        }
        if (loginMudou && repository.existsByLogin(dto.getLogin())) {
            throw new RuntimeException("Login já cadastrado em outra conta.");
        }

        funcionarioExistente.setCargo(dto.getCargo());
        funcionarioExistente.setNomeCompleto(dto.getNomeCompleto());
        funcionarioExistente.setEmail(dto.getEmail());
        funcionarioExistente.setTelefone(dto.getTelefone());
        funcionarioExistente.setLogin(dto.getLogin());

        if (dto.getSenha() != null && !dto.getSenha().isBlank()) {
            funcionarioExistente.setSenhaCriptografada(passwordEncoder.encode(dto.getSenha()));
        }

        Funcionario salvo = repository.save(funcionarioExistente);

        return new FuncionarioResponseDTO(
                salvo.getIdFuncionario(),
                salvo.getCargo(),
                salvo.getNomeCompleto(),
                salvo.getCpf(),
                salvo.getEmail(),
                salvo.getTelefone(),
                salvo.getLogin(),
                salvo.isAtivo()
        );
    }


    @Transactional
    public FuncionarioResponseDTO atualizarParcial(Long id, FuncionarioPatchDTO patchDto) {
        Funcionario funcionarioExistente = buscarPorId(id);

        if (patchDto.getCargo() != null) {
            funcionarioExistente.setCargo(patchDto.getCargo());
        }

        if (patchDto.getNomeCompleto() != null && !patchDto.getNomeCompleto().isBlank()) {
            funcionarioExistente.setNomeCompleto(patchDto.getNomeCompleto());
        }



        if (patchDto.getEmail() != null && !patchDto.getEmail().isBlank()) {

            boolean emailMudouParcial = false;
            if (funcionarioExistente.getEmail() == null) {
                emailMudouParcial = true;
            } else {
                emailMudouParcial = !patchDto.getEmail().equalsIgnoreCase(funcionarioExistente.getEmail());
            }
            if (emailMudouParcial && repository.existsByEmail(patchDto.getEmail())) {
                throw new RuntimeException("E-mail já cadastrado em outra conta.");
            }
            funcionarioExistente.setEmail(patchDto.getEmail());
        }

        if (patchDto.getTelefone() != null && !patchDto.getTelefone().isBlank()) {
            funcionarioExistente.setTelefone(patchDto.getTelefone());
        }

        // Lógica para Login (com validação)
        if (patchDto.getLogin() != null && !patchDto.getLogin().isBlank()) {

            boolean loginMudouParcial = false;
            if (funcionarioExistente.getLogin() == null) {
                loginMudouParcial = true;
            } else {
                loginMudouParcial = !patchDto.getLogin().equals(funcionarioExistente.getLogin());
            }

            if (loginMudouParcial && repository.existsByLogin(patchDto.getLogin())) {
                throw new RuntimeException("Login já cadastrado em outra conta.");
            }
            funcionarioExistente.setLogin(patchDto.getLogin());
        }

        if (patchDto.getSenha() != null && !patchDto.getSenha().isBlank()) {
            funcionarioExistente.setSenhaCriptografada(passwordEncoder.encode(patchDto.getSenha()));
        }

        Funcionario salvo = repository.save(funcionarioExistente);

        return new FuncionarioResponseDTO(
                salvo.getIdFuncionario(),
                salvo.getCargo(),
                salvo.getNomeCompleto(),
                salvo.getCpf(),
                salvo.getEmail(),
                salvo.getTelefone(),
                salvo.getLogin(),
                salvo.isAtivo()
        );
    }

    @Transactional
    public void deletar(Long id) {
        // Deleção lógica
        Funcionario funcionario = buscarPorId(id);
        if (!funcionario.isAtivo()) {
            throw new RuntimeException("Funcionário já está inativo.");
        }
        // Marca como inativo
        funcionario.setAtivo(false);
        // Salva a alteração
        repository.save(funcionario);
    }
}

