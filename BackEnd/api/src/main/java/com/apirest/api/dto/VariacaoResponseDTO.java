package com.apirest.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VariacaoResponseDTO {
    private Long id;
    private String nomeVariacao; // "Azul P"
    private String nomeCompleto; // "Camiseta Nike - Azul P"
    private String sku;
    private String codigoBarras;
    private BigDecimal precoCusto;
    private BigDecimal precoVenda;

    // Estoque Geral
    private Integer estoqueAtual;
    private String statusEstoque; // "Disponível", "Esgotado"
    private boolean ativo;
}