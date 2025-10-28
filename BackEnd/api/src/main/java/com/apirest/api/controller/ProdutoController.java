package com.apirest.api.controller;


import com.apirest.api.dto.*;
import com.apirest.api.service.ProdutoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/produtos")
@RequiredArgsConstructor
public class ProdutoController {

    private final ProdutoService service;


    // POST para criar um novo produto
    @PostMapping
    public ResponseEntity<ProdutoResponseDTO> criar(@Valid @RequestBody ProdutoDTO dto) {
        ProdutoResponseDTO response = service.criarProduto(dto);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    // PUT para atualizar informações gerais do produto
    @PutMapping("/{id}")
    public ResponseEntity<ProdutoResponseDTO> atualizarInformacoes(
            @PathVariable Long id,
            @Valid @RequestBody ProdutoUpdateDTO dto) {
        ProdutoResponseDTO response = service.atualizarInformacoes(id, dto);
        return ResponseEntity.ok(response);
    }

    // PATCH para atualizar preço, estoque e descrição
    @PatchMapping("/{id}")
    public ResponseEntity<ProdutoResponseDTO> atualizarParcialmente(
            @PathVariable Long id,
            @Valid @RequestBody ProdutoPrecoEstoqueUpdateDTO dto) {
        ProdutoResponseDTO response = service.atualizarPrecoEEstoque(id, dto);
        return ResponseEntity.ok(response);
    }

    // Lista todos os produtos ativos
    @GetMapping
    public ResponseEntity<List<ProdutoResponseDTO>> listarAtivos() {
        return ResponseEntity.ok(service.listarProdutosAtivos());
    }

    // Busca produto por ID
    @GetMapping("/{id}")
    public ResponseEntity<ProdutoResponseDTO> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(service.getProdutoResponseById(id));
    }

    // DELETE lógico de um produto
    @DeleteMapping("/{idProduto}/{idFuncionario}")
    public ResponseEntity<Void> deletarProduto(
            @PathVariable Long idProduto,
            @PathVariable Long idFuncionario) {
        service.deletarProdutoLogicamente(idProduto, idFuncionario);
        return ResponseEntity.noContent().build();
    }
}

