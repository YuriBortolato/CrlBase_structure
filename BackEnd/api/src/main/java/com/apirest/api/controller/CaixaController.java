package com.apirest.api.controller;

import com.apirest.api.dto.*;
import com.apirest.api.entity.Caixa;
import com.apirest.api.entity.FiltroPeriodo;
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
    public ResponseEntity<CaixaResponseDTO> abrirCaixa(@Valid @RequestBody CaixaAberturaDTO dto) {
        Caixa caixaAberto = caixaService.abrirCaixa(dto);
        return new ResponseEntity<>(caixaService.toResponseDTO(caixaAberto), HttpStatus.CREATED);
    }

    @PostMapping("/fechar/{id}")
    public ResponseEntity<CaixaResponseDTO> fecharCaixa(@PathVariable Long id, @Valid @RequestBody CaixaFechamentoDTO dto) {
        // Fecha o caixa e recebe a entidade
        Caixa caixaFechado = caixaService.fecharCaixa(id, dto);

        // Converte para DTO
        CaixaResponseDTO response = caixaService.toResponseDTO(caixaFechado);

        return ResponseEntity.ok(response);
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

    // RELATÓRIO AVANÇADO
    @GetMapping("/relatorio")
    public ResponseEntity<Map<String, Object>> getRelatorio(
            @RequestHeader("id-solicitante") Long idSolicitante, // SIMULA LOGIN: Quem está pedindo?

            @RequestParam(required = false) Long idFuncionario, // Filtro opcional
            @RequestParam(required = false, defaultValue = "HOJE") FiltroPeriodo periodo, // HOJE, ONTEM, DIAS_7...
            @RequestParam(required = false) StatusCaixa status, // ABERTO, FECHADO (Null = Todos)

            // Apenas se periodo for CUSTOM
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataFim
    ) {
        // Validação simples de segurança para teste
        if (idSolicitante == null) {
            throw new RuntimeException("Header 'id-solicitante' é obrigatório para identificar quem pede o relatório.");
        }

        Map<String, Object> resultado = caixaService.gerarRelatorioAvancado(
                idSolicitante,
                idFuncionario,
                periodo,
                status,
                dataInicio,
                dataFim
        );

        return ResponseEntity.ok(resultado);
    }

    // Relatório do Dia
    @GetMapping("/diario")
    public ResponseEntity<RelatorioPeriodoDTO> relatorioDiario(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data) {
        if (data == null) data = LocalDate.now();
        return ResponseEntity.ok(caixaService.gerarRelatorioDoDia(data));
    }

    // Relatório do Mês
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
