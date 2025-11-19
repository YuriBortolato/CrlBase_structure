package com.apirest.api.controller;

import com.apirest.api.dto.*;
import com.apirest.api.entity.Caixa;
import com.apirest.api.entity.StatusCaixa;
import com.apirest.api.service.CaixaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/caixas")
@RequiredArgsConstructor
public class CaixaController {

    private final CaixaService caixaService;

    @PostMapping("/abrir")
    public ResponseEntity<Caixa> abrirCaixa(@Valid @RequestBody CaixaAberturaDTO dto) {
        return new ResponseEntity<>(caixaService.abrirCaixa(dto), HttpStatus.CREATED);
    }

    @PostMapping("/fechar/{id}")
    public ResponseEntity<Caixa> fecharCaixa(@PathVariable Long id, @Valid @RequestBody CaixaFechamentoDTO dto) {
        return ResponseEntity.ok(caixaService.fecharCaixa(id, dto));
    }

    // Endpoint para listar caixas com filtros e resumo
    @GetMapping
    public ResponseEntity<Map<String, Object>> listarCaixas(
            @RequestParam(required = false) Long idFuncionario,
            @RequestParam(required = false) StatusCaixa status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataFim
    ) {
        LocalDateTime inicio = (dataInicio != null) ? dataInicio.atStartOfDay() : LocalDate.now().atStartOfDay();
        LocalDateTime fim = (dataFim != null) ? dataFim.atTime(LocalTime.MAX) : LocalDate.now().atTime(LocalTime.MAX);

        List<Caixa> listaEntidades = caixaService.listarComFiltros(idFuncionario, status, inicio, fim);

        // Converte para DTO leve
        List<CaixaResponseDTO> listaDTOs = listaEntidades.stream()
                .map(caixaService::toResponseDTO)
                .toList();

        DashboardResumoDTO resumo = caixaService.calcularResumoDashboard(listaEntidades);

        Map<String, Object> response = new HashMap<>();
        response.put("resumo", resumo);
        response.put("caixas", listaDTOs);

        return ResponseEntity.ok(response);
    }

    //  Relatório do Dia
    // Ex: GET /caixas/diario?data=2025-11-19
    @GetMapping("/diario")
    public ResponseEntity<RelatorioPeriodoDTO> relatorioDiario(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data) {

        if (data == null) data = LocalDate.now();
        return ResponseEntity.ok(caixaService.gerarRelatorioDoDia(data));
    }

    // Relatório do Mês
    // Ex: GET /caixas/mensal?mes=11&ano=2025
    @GetMapping("/mensal")
    public ResponseEntity<RelatorioPeriodoDTO> relatorioMensal(
            @RequestParam(defaultValue = "0") int mes,
            @RequestParam(defaultValue = "0") int ano) {

        LocalDate hoje = LocalDate.now();
        if (mes == 0) mes = hoje.getMonthValue();
        if (ano == 0) ano = hoje.getYear();

        return ResponseEntity.ok(caixaService.gerarRelatorioDoMes(ano, mes));
    }
}