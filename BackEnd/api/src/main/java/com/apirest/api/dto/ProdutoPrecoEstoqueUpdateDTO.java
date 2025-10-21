package com.apirest.api.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProdutoPrecoEstoqueUpdateDTO {

    @NotNull(message = "idFuncionario é obrigatório")
    private Long idFuncionario;

    @NotNull(message = "O valor de custo é obrigatório.")
    @DecimalMin(value = "0.0", inclusive = true)
    private BigDecimal valorCusto;

    @NotNull(message = "O valor de venda é obrigatório.")
    @DecimalMin(value = "0.01", inclusive = true)
    private BigDecimal valorVenda;

    @NotNull(message = "A quantidade em estoque é obrigatória.")
    @Min(value = 0)
    private Integer quantidadeEmEstoque;
}
