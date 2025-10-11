package com.apirest.api.dto;


import jakarta.validation.constraints.*;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProdutoUpdateDTO {

    @NotNull(message = "idFuncionario é obrigatório")
    private Long idFuncionario;

    @NotBlank(message = "Nome é obrigatório")
    private String nome;

    @NotBlank(message = "Categoria é obrigatória")
    private String categoria;

    @NotBlank(message = "Descrição é obrigatória")
    private String descricao;
}