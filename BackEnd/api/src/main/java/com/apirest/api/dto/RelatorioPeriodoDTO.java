package com.apirest.api.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
public class RelatorioPeriodoDTO {
    private String periodo; // "19/11/2025" ou "NOVEMBRO/2025"
    private BigDecimal totalGeral;
    private BigDecimal totalDinheiro;
    private BigDecimal totalPix;
    private BigDecimal totalDebito;
    private BigDecimal totalCredito;
    private BigDecimal totalCrediario;
    private Integer quantidadeCaixasFechados;
}