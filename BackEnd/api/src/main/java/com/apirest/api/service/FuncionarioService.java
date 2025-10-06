package com.apirest.api.service;

import com.apirest.api.dto.FuncionarioDTO;
import com.apirest.api.dto.FuncionarioResponseDTO;
import com.apirest.api.entity.Funcionario;
import com.apirest.api.repository.FuncionarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FuncionarioService {

    private final FuncionarioRepository repository;

    public FuncionarioResponseDTO criar(FuncionarioDTO dto) {
        if (repository.existsByEmail(dto.getEmail())) {
            throw new RuntimeException("Email já cadastrado: " + dto.getEmail());
        }

        Funcionario funcionario = new Funcionario(null, dto.getEmail(), dto.getNome(), dto.getSenha());
        Funcionario salvo = repository.save(funcionario);

        return new FuncionarioResponseDTO(salvo.getId(), salvo.getEmail(), salvo.getNome());
    }

    public List<Funcionario> listarTodos() {
        return repository.findAll();
    }

    public Funcionario buscarPorId(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Funcionário não encontrado com id " + id));
    }
}
