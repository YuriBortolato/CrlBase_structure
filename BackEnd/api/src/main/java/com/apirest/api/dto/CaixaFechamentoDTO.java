package com.apirest.api.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class CaixaFechamentoDTO {
    @NotNull
    private Long idFuncionario; // Quem está fechando o caixa
    @NotNull
    @Min(0)
    private BigDecimal dinheiro;

    @NotNull
    @Min(0)
    private BigDecimal transferencia; // Pix

    @NotNull
    @Min(0)
    private BigDecimal debito;

    @NotNull
    @Min(0)
    private BigDecimal credito;

    @NotNull
    @Min(0)
    private BigDecimal crediario;

    private String observacao;
}