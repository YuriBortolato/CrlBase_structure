package com.apirest.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
public class CadastroProdutoDTO {

    @NotNull(message = "ID do Funcionário é obrigatório")
    private Long idFuncionario;

    // Dados do Produto
    @NotBlank(message = "Nome genérico é obrigatório")
    private String nomeGenerico;

    private String marca;
    private String descricao;

    @NotBlank(message = "NCM é obrigatório")
    private String ncm;

    @NotBlank(message = "Categoria é obrigatória")
    private String nomeCategoria;

    // Dados das Variações
    @Valid
    private List<VariacaoDTO> variacoes;

    @Data
    public static class VariacaoDTO {
        @NotBlank(message = "Nome da variação é obrigatório")
        private String nomeVariacao; // Ex: "Tamanho M", "Cor Vermelha"

        private String codigoBarras;

        @NotNull
        private BigDecimal precoCusto;

        @NotNull
        private BigDecimal precoVenda;

        private Integer estoqueInicial;
        private Integer estoqueMinimo;
    }
}