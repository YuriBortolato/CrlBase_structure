package com.apirest.api.controller;

import com.apirest.api.dto.FuncionarioDTO;
import com.apirest.api.dto.FuncionarioPatchDTO;
import com.apirest.api.dto.FuncionarioResponseDTO;
import com.apirest.api.entity.Funcionario;
import com.apirest.api.service.FuncionarioService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/funcionarios")
@RequiredArgsConstructor
public class FuncionarioController {

    private final FuncionarioService service;

    @PostMapping
    public ResponseEntity<FuncionarioResponseDTO> criar(@Valid @RequestBody FuncionarioDTO dto) {
        return ResponseEntity.ok(service.criar(dto));
    }

    @GetMapping
    public ResponseEntity<List<Funcionario>> listar() {
        return ResponseEntity.ok(service.listarTodos());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Funcionario> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(service.buscarPorId(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<FuncionarioResponseDTO> atualizar(@PathVariable Long id, @Valid @RequestBody FuncionarioDTO dto) {
        return ResponseEntity.ok(service.atualizar(id, dto));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<FuncionarioResponseDTO> atualizarParcial(@PathVariable Long id, @Valid @RequestBody FuncionarioPatchDTO patchDto) {
        return ResponseEntity.ok(service.atualizarParcial(id, patchDto));
    }

    @PatchMapping("/{id}/pin")
    public ResponseEntity<Void> alterarPin(@PathVariable Long id, @RequestBody Map<String, String> payload) {
        String novoPin = payload.get("pin");
        if (novoPin == null || novoPin.length() < 4) {
            throw new RuntimeException("PIN inválido. Mínimo 4 dígitos.");
        }

        service.atualizarPin(id, novoPin); // Você precisará criar esse método no Service
        return ResponseEntity.noContent().build();
    }

    // Rota dedicada para alterar o limite
    @PatchMapping("/{id}/limite")
    public ResponseEntity<Void> atualizarLimite(@PathVariable Long id, @RequestBody Map<String, java.math.BigDecimal> payload) {
        java.math.BigDecimal novoLimite = payload.get("limite");

        if (novoLimite == null || novoLimite.compareTo(java.math.BigDecimal.ZERO) < 0) {
            throw new RuntimeException("O limite deve ser um valor positivo.");
        }

        service.atualizarLimiteCredito(id, novoLimite);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(@PathVariable Long id) {
        service.deletar(id);
        return ResponseEntity.noContent().build();
    }

}
