package com.apirest.api.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import java.math.BigDecimal;

@Entity
@Table(name = "produtos_variacoes")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProdutoVariacao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "produto_pai_id")
    private ProdutoPai produtoPai;

    private String nomeVariacao; // Ex: "Vermelho", "M"

    // Ex: "Camiseta Dry-Fit - Vermelho M"
    private String nomeCompletoConcatenado;

    @Column(unique = true)
    private String codigoBarras; // EAN-13

    @Column(unique = true)
    private String sku; // Stock Keeping Unit

    @NotNull
    private BigDecimal precoCusto;

    @NotNull
    private BigDecimal precoVenda;

    @Builder.Default
    private boolean ativo = true;
}