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
public class DashboardResumoDTO {
    // Valores gerais
    private BigDecimal totalPrevisto;   // O que o SISTEMA prevê
    private BigDecimal totalConferido;  // O que o FUNCIONÁRIO informou na conferência
    private BigDecimal totalQuebra;     // Diferença entre previsto e conferido

    // Valores por forma de pagamento
    private BigDecimal totalDinheiro;
    private BigDecimal totalPix;
    private BigDecimal totalDebito;
    private BigDecimal totalCredito;
    private BigDecimal totalCrediario;

    // Valores previstos por forma de pagamento
    private BigDecimal previstoDinheiro;
    private BigDecimal previstoPix;
    private BigDecimal previstoDebito;
    private BigDecimal previstoCredito;
    private BigDecimal previstoCrediario;
}