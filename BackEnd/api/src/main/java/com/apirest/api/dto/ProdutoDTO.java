package com.apirest.api.dto;

import jakarta.persistence.Column;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProdutoDTO {

    @NotNull(message = "idFuncionario é obrigatório")
    private Long idFuncionario;

    @NotBlank(message = "O nome do produto é obrigatório.")
    @Size(min = 2, max = 100, message = "O nome do produto deve ter entre 2 e 100 caracteres.")
    @Column(name = "nome", nullable = false, length = 100, unique = true) // deve corrigir mais tarde --------------------------
    private String nome;

    @NotBlank(message = "A categoria é obrigatória.")
    @Size(min = 2, max = 100, message = "O nome da categoria deve ter entre 2 e 100 caracteres.")
    private String categoria;

    @NotNull(message = "O valor de custo é obrigatório.")
    @DecimalMin(value = "0.0", inclusive = true, message = "O valor de custo deve ser maior ou igual a 0.")
    private BigDecimal valorCusto;

    @NotNull(message = "O valor de venda é obrigatório.")
    @DecimalMin(value = "0.01", inclusive = true, message = "O valor de venda deve ser maior que 0.")
    private BigDecimal valorVenda;

    @NotBlank(message = "A descrição é obrigatória.")
    @Size(min = 5, max = 500, message = "A descrição deve ter entre 5 e 500 caracteres.")
    private String descricao;

    @NotNull(message = "A quantidade em estoque é obrigatória.")
    @Min(value = 0, message = "A quantidade em estoque não pode ser negativa.")
    private Integer quantidadeEmEstoque;

    @NotNull(message = "A quantidade mínima é obrigatória.")
    @Min(value = 0, message = "A quantidade mínima em estoque não pode ser negativa.")
    private Integer quantidadeMinima;
}
