package com.apirest.api.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class CaixaAberturaDTO {
    @NotNull(message = "ID do funcionário é obrigatório")
    private Long idFuncionario;

    @NotNull(message = "Saldo inicial é obrigatório")
    private BigDecimal saldoInicial;

    private String observacao;
}