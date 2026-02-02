package com.apirest.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProdutoResponseDTO {
    // Atributos do PRODUTO
    private Long id;
    private String nomeGenerico;
    private String marca;
    private String descricao;
    private String ncm;
    private String categoria;
    private boolean ativo;

    // Atributos das VARIAÇÕES do produto
    private List<VariacaoResponseDTO> variacoes;
}