package com.apirest.api.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

@Entity
@Table(name = "produtos")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Produto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_produto")
    private Long idProduto;

    @Column(nullable = false)
    private String nome;

    @ManyToOne(optional = false)
    @JoinColumn(name = "id_categoria", nullable = false)
    private Categoria categoria;

    @Column(name = "valor_custo", nullable = false)
    private Double valorCusto;

    @Column(name = "valor_venda", nullable = false)
    private Double valorVenda;

    @Column(nullable = false)
    private String descricao;

    // quantidade atual, sempre atualizada
    @Column(name = "quantidade_estoque", nullable = false)
    private Integer quantidadeEmEstoque;
}