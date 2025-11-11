package com.apirest.api.controller;


import com.apirest.api.dto.VendaDTO;
import com.apirest.api.dto.VendaResponseDTO;
import com.apirest.api.service.VendaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.apirest.api.entity.StatusVenda;

import java.util.List;

@RestController
@RequestMapping("/vendas")
@RequiredArgsConstructor
public class VendaController {

    private final VendaService vendaService;

    // Registrar uma nova venda
    @PostMapping
    public ResponseEntity<VendaResponseDTO> registrarVenda(@Valid @RequestBody VendaDTO vendaDTO) {
        VendaResponseDTO novaVenda = vendaService.registrarVenda(vendaDTO);
        return new ResponseEntity<>(novaVenda, HttpStatus.CREATED);
    }

    // Listar todas as vendas
    @GetMapping
    public ResponseEntity<List<VendaResponseDTO>> listarVendas(
            @RequestParam(required = false) StatusVenda status) {
        List<VendaResponseDTO> vendas = vendaService.listarVendas(status);
        return ResponseEntity.ok(vendas);
    }

    // Buscar venda por ID
    @GetMapping("/{id}")
    public ResponseEntity<VendaResponseDTO> buscarVendaPorId(@PathVariable Long id) {
        VendaResponseDTO venda = vendaService.buscarPorId(id);
        return ResponseEntity.ok(venda);
    }

    // NOVO: Cancelar (Delete Lógico)
    @DeleteMapping("/{idVenda}/{idFuncionario}")
    public ResponseEntity<VendaResponseDTO> cancelarVenda(
            @PathVariable Long idVenda,
            @PathVariable Long idFuncionario) {
        VendaResponseDTO vendaCancelada = vendaService.cancelarVenda(idVenda, idFuncionario);
        return ResponseEntity.ok(vendaCancelada);
    }

    // NOVO: Reativar Venda
    @PatchMapping("/{idVenda}/reativar/{idFuncionario}")
    public ResponseEntity<VendaResponseDTO> reativarVenda(
            @PathVariable Long idVenda,
            @PathVariable Long idFuncionario) {
        VendaResponseDTO vendaReativada = vendaService.reativarVenda(idVenda, idFuncionario);
        return ResponseEntity.ok(vendaReativada);
    }
}
