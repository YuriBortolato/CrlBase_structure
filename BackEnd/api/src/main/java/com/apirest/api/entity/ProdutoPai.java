package com.apirest.api.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "produtos_pai")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProdutoPai {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Nome genérico é obrigatório")
    private String nomeGenerico; // Ex: "Camiseta Dry-Fit"

    private String marca; // Ex: "Nike"

    @Column(columnDefinition = "TEXT")
    private String descricao;

    @NotBlank(message = "NCM é obrigatório para Nota Fiscal")
    @Column(length = 8)
    private String ncm; // Ex: "61091000"

    @ManyToOne
    @JoinColumn(name = "categoria_id")
    private Categoria categoria;

    @Builder.Default
    private boolean ativo = true;

    // Relação com variações
    @OneToMany(mappedBy = "produtoPai", cascade = CascadeType.ALL)
    private List<ProdutoVariacao> variacoes = new ArrayList<>();
}