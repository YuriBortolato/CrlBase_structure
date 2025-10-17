package com.apirest.api.dto;


import jakarta.validation.constraints.*;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProdutoUpdateDTO {

    @NotNull(message = "idFuncionario é obrigatório")
    private Long idFuncionario;

    @NotBlank(message = "O nome do produto é obrigatório.")
    @Size(min = 2, max = 100, message = "O nome do produto deve ter entre 2 e 100 caracteres.")
    private String nome;


    @NotBlank(message = "A categoria é obrigatória.")
    @Size(min = 2, max = 100, message = "O nome da categoria deve ter entre 2 e 100 caracteres.")
    private String categoria;


    @NotBlank(message = "A descrição é obrigatória.")
    @Size(min = 5, max = 500, message = "A descrição deve ter entre 5 e 500 caracteres.")
    private String descricao;
}