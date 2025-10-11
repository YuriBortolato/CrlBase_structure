package com.apirest.api.dto;

import jakarta.validation.constraints.*;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProdutoDTO {

    // id do funcionário que está fazendo a operação (substituir por auth real depois)
    @NotNull(message = "idFuncionario é obrigatório")
    private Long idFuncionario;

    @NotBlank(message = "Nome é obrigatório")
    private String nome;

    // pode enviar nome da categoria (string) — se existir será usada, senão criada
    @NotBlank(message = "Categoria é obrigatória")
    private String categoria;

    @NotNull(message = "valorCusto é obrigatório")
    @PositiveOrZero(message = "valorCusto deve ser >= 0")
    private Double valorCusto;

    @NotNull(message = "valorVenda é obrigatório")
    @Positive(message = "valorVenda deve ser > 0")
    private Double valorVenda;

    @NotBlank(message = "Descrição é obrigatória")
    private String descricao;

    @NotNull(message = "quantidadeEmEstoque é obrigatória")
    @Min(value = 0, message = "quantidadeEmEstoque não pode ser negativa")
    private Integer quantidadeEmEstoque;
}
