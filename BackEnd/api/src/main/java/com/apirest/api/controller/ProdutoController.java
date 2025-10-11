package com.apirest.api.controller;


import com.apirest.api.dto.*;
import com.apirest.api.entity.Produto;
import com.apirest.api.service.ProdutoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/produtos")
@RequiredArgsConstructor
public class ProdutoController {

    private final ProdutoService service;

    // criar produto (só cargos autorizados)
    @PostMapping
    public ResponseEntity<ProdutoResponseDTO> criar(@Valid @RequestBody ProdutoDTO dto) {
        ProdutoResponseDTO response = service.criarProduto(dto);
        return ResponseEntity.ok(response);
    }

    // atualizar (nome, descricao, categoria) - enviar idFuncionario dentro do DTO
    @PutMapping("/{id}")
    public ResponseEntity<ProdutoResponseDTO> atualizar(
            @PathVariable Long id,
            @Valid @RequestBody ProdutoUpdateDTO dto) {
        ProdutoResponseDTO response = service.atualizarProduto(id, dto);
        return ResponseEntity.ok(response);
    }

    // listar público — todos (clientes e funcionários) podem ver
    @GetMapping
    public ResponseEntity<List<ProdutoResponseDTO>> listarPublico() {
        return ResponseEntity.ok(service.listarProdutosPublico());
    }

    // buscar por id (detalhes)
    @GetMapping("/{id}")
    public ResponseEntity<ProdutoResponseDTO> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(service.getProdutoResponseById(id));
    }
}
