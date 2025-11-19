package com.apirest.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RelatorioIndividualDTO {
    private String nomeFuncionario;

    // --- HOJE ---
    private BigDecimal totalGeralHoje;
    private BigDecimal dinheiroHoje;
    private BigDecimal pixHoje;
    private BigDecimal debitoHoje;
    private BigDecimal creditoHoje;
    private BigDecimal crediarioHoje;

    // --- MÊS ---
    private BigDecimal totalGeralMes;
    private BigDecimal dinheiroMes;
    private BigDecimal pixMes;
    private BigDecimal debitoMes;
    private BigDecimal creditoMes;
    private BigDecimal crediarioMes;
}