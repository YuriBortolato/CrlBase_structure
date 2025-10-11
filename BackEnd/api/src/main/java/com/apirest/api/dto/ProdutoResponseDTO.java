package com.apirest.api.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProdutoResponseDTO {
    private Long idProduto;
    private String nome;
    private String descricao;
    private String categoria;
    private Double valorVenda;
    private Integer quantidadeEmEstoque;
    private String statusEstoque; // "Esgotado", "Quase Esgotado", "Disponível"
}

