package com.apirest.api.service;

import com.apirest.api.dto.ProdutoDTO;
import com.apirest.api.dto.ProdutoResponseDTO;
import com.apirest.api.entity.Produto;
import com.apirest.api.repository.ProdutoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProdutoService {

    private final ProdutoRepository repository;

    public ProdutoResponseDTO criar(ProdutoDTO dto) {
        if (repository.existsByNome(dto.getNome())) {
            throw new RuntimeException("Produto já cadastrado: " + dto.getNome());
        }

        Produto Produto = new Produto(null, dto.getNome(), dto.getPreco(), dto.getDescricao());
        Produto salvo = repository.save(Produto);

        return new ProdutoResponseDTO(salvo.getId(), salvo.getNome(), salvo.getPreco(), salvo.getDescricao());
    }

    public List<Produto> listarTodos() {
        return repository.findAll();
    }

    public Produto buscarPorId(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Produto não encontrado com id " + id));
    }
}
