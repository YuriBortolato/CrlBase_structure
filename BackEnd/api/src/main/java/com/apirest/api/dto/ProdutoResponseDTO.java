package com.apirest.api.dto;

import lombok.*;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProdutoResponseDTO {
    private Long idProduto;
    private String nome;
    private String descricao;
    private String categoria;
    private BigDecimal valorVenda;
    private Integer quantidadeEmEstoque;
    private String statusEstoque; // "Esgotado", "Quase Esgotado", "Disponível"
    private boolean ativo;
}

