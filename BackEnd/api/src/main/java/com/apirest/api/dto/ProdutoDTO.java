package com.apirest.api.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProdutoDTO {
    private String nome;
    private Double preco;
    private String descricao;
}
