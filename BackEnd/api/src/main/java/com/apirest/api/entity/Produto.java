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

    @NotBlank(message = "O nome do produto é obrigatório.")
    @Pattern(regexp = "^[A-Za-zÀ-ÿ0-9\\s]{3,100}$", message = "O nome deve conter entre 3 e 100 caracteres e apenas letras, números e espaços.")
    @Column(nullable = false, length = 100)
    private String nome;

    @ManyToOne(optional = false)
    @JoinColumn(name = "id_categoria", nullable = false)
    @NotNull(message = "A categoria é obrigatória.")
    private Categoria categoria;

    // custo deve ser menor que venda
    @NotNull(message = "O valor de custo é obrigatório.")
    @DecimalMin(value = "0.0", inclusive = false, message = "O valor de custo deve ser maior que zero.")
    @Digits(integer = 10, fraction = 2, message = "O valor de custo deve ter no máximo 10 dígitos inteiros e 2 decimais.")
    @Column(name = "valor_custo", nullable = false)
    private Double valorCusto;

    // venda deve ser maior que custo
    @NotNull(message = "O valor de venda é obrigatório.")
    @DecimalMin(value = "0.0", inclusive = false, message = "O valor de venda deve ser maior que zero.") // <-- ADICIONADO
    @Digits(integer = 10, fraction = 2, message = "O valor de venda deve ter no máximo 10 dígitos inteiros e 2 decimais.") // <-- ADICIONADO
    @Column(name = "valor_venda", nullable = false)
    private Double valorVenda;


    @NotBlank(message = "A descrição é obrigatória.")
    @Size(min = 5, max = 500, message = "A descrição deve ter entre 5 e 500 caracteres.")
    @Column(nullable = false, length = 500)
    private String descricao;

    // quantidade atual, sempre atualizada
    @NotNull(message = "A quantidade em estoque é obrigatória.")
    @Min(value = 0, message = "A quantidade em estoque não pode ser negativa.")
    @Column(name = "quantidade_estoque", nullable = false)
    private Integer quantidadeEmEstoque;
}