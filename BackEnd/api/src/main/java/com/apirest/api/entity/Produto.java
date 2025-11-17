package com.apirest.api.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;

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

    // nome do produto
    @NotBlank(message = "O nome do produto é obrigatório.")
    @Pattern(regexp = "^[A-Za-zÀ-ÿ0-9\\s.,'\"()&/@-]{3,255}$",
            message = "O nome deve conter entre 3 e 255 caracteres e não deve incluir símbolos de controle.")
    @Column(nullable = false, length = 255, unique = true)
    private String nome;

    // categoria do produto
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "id_categoria", nullable = false)
    @NotNull(message = "A categoria é obrigatória.")
    private Categoria categoria;

    // custo deve ser menor que venda
    @NotNull(message = "O valor de custo é obrigatório.")
    @DecimalMin(value = "0.0", inclusive = true, message = "O valor de custo deve ser maior ou igual a zero.")
    @Digits(integer = 10, fraction = 2)
    @Column(name = "valor_custo", nullable = false, precision = 12, scale = 2)
    private BigDecimal valorCusto;

    // venda deve ser maior que custo
    @NotNull(message = "O valor de venda é obrigatório.")
    @DecimalMin(value = "0.01", inclusive = true, message = "O valor de venda deve ser maior que zero.")
    @Digits(integer = 10, fraction = 2)
    @Column(name = "valor_venda", nullable = false, precision = 12, scale = 2)
    private BigDecimal valorVenda;

    // descrição do produto
    @NotBlank(message = "A descrição é obrigatória.")
    @Size(min = 5, max = 500, message = "A descrição deve ter entre 5 e 500 caracteres.")
    @Column(nullable = false, length = 500)
    private String descricao;

    // quantidade atual, sempre atualizada
    @NotNull(message = "A quantidade em estoque é obrigatória.")
    @Min(value = 0, message = "A quantidade em estoque não pode ser negativa.")
    @Column(name = "quantidade_estoque", nullable = false)
    private Integer quantidadeEmEstoque;

    // quantidade mínima para alerta de estoque baixo
    @NotNull(message = "A quantidade mínima em estoque é obrigatória.")
    @Min(value = 0, message = "A quantidade mínima em estoque não pode ser negativa.")
    @Column(name = "quantidade_minima", nullable = false)
    private Integer quantidadeMinima;

    // Indica se o produto está ativo ou inativo
    @Column(nullable = false)
    private boolean ativo = true;
}