package com.apirest.api.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "estoque_saldos", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"unidade_id", "produto_variacao_id"})
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EstoqueSaldo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "unidade_id")
    private Unidade unidade;

    @ManyToOne(optional = false)
    @JoinColumn(name = "produto_variacao_id")
    private ProdutoVariacao produtoVariacao;

    @Builder.Default
    private Integer quantidadeAtual = 0;

    @Builder.Default
    private Integer quantidadeMinima = 5;
}