package com.apirest.api.controller;

import com.apirest.api.dto.ContaReceberResponseDTO;
import com.apirest.api.service.ContaReceberService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/contas-receber")
@RequiredArgsConstructor
public class ContaReceberController {

    private final ContaReceberService service;

    // Rota: GET /contas-receber/cliente/1
    // Busca todas as contas a receber associadas a um cliente específico pelo ID do cliente
    @GetMapping("/cliente/{idCliente}")
    public ResponseEntity<List<ContaReceberResponseDTO>> listarPorCliente(@PathVariable Long idCliente) {
        return ResponseEntity.ok(service.buscarPorCliente(idCliente));
    }

    // Rota: GET /contas-receber/50
    // Busca uma conta a receber específica pelo ID
    @GetMapping("/{idConta}")
    public ResponseEntity<ContaReceberResponseDTO> buscarPorId(@PathVariable Long idConta) {
        return ResponseEntity.ok(service.buscarPorId(idConta));
    }
}