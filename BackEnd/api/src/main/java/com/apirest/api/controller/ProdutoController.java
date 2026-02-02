package com.apirest.api.controller;

import com.apirest.api.dto.CadastroProdutoDTO;
import com.apirest.api.dto.ProdutoResponseDTO;
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

    // CRIAR (POST)
    @PostMapping
    public ResponseEntity<Void> criar(@Valid @RequestBody CadastroProdutoDTO dto) {
        service.cadastrarProdutoCompleto(dto);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    // LISTAR TUDO (GET)
    @GetMapping
    public ResponseEntity<List<ProdutoResponseDTO>> listar(
            @RequestParam(required = false) Long idFuncionario) {
        return ResponseEntity.ok(service.listarTudo(idFuncionario));
    }

    // BUSCAR POR ID (GET)
    @GetMapping("/{id}")
    public ResponseEntity<ProdutoResponseDTO> buscarPorId(
            @PathVariable Long id,
            @RequestParam(required = false) Long idFuncionario) {
        return ResponseEntity.ok(service.buscarPorId(id, idFuncionario));
    }

    // ATUALIZAR (PUT)
    @PutMapping("/{id}")
    public ResponseEntity<Void> atualizar(
            @PathVariable Long id,
            @Valid @RequestBody CadastroProdutoDTO dto) {
        service.atualizarProdutoCompleto(id, dto);
        return ResponseEntity.ok().build();
    }

    // DELETAR (DELETE LÓGICO)
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(
            @PathVariable Long id,
            @RequestParam Long idFuncionario) {
        service.deletarProdutoLogicamente(id, idFuncionario);
        return ResponseEntity.noContent().build();
    }
}