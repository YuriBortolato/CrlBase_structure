package com.apirest.api.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProdutoResponseDTO {
    private Long id;
    private String nome;
    private Double preco;
    private String descricao;
}
