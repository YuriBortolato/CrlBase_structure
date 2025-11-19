package com.apirest.api.dto;

import com.apirest.api.entity.StatusCaixa;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CaixaResponseDTO {
    // --- Dados do caixa ---
    private Long idCaixa;
    private Long idFuncionario;
    private String nomeFuncionario;
    private LocalDateTime dataAbertura;
    private LocalDateTime dataFechamento;
    private StatusCaixa status;
    private BigDecimal saldoInicial;

    // --- Valores informados pelo funcionario no fechamento ---
    private BigDecimal conferidoDinheiro;
    private BigDecimal conferidoPix;
    private BigDecimal conferidoDebito;
    private BigDecimal conferidoCredito;
    private BigDecimal conferidoCrediario;

    // --- Valores calculados pelo sistema ---
    private BigDecimal quebraDeCaixa;
    private String observacoes;
}