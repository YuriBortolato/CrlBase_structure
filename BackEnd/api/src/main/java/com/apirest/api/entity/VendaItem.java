package com.apirest.api.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "venda_item")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VendaItem {

    // Primary Key
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_venda_item")
    private Long idVendaItem;

    // Foreign Keys
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_venda", nullable = false)
    private Venda venda;

    // Produto associado ao item da venda
    @ManyToOne(optional = false)
    @JoinColumn(name = "id_produto_variacao", nullable = false)
    private ProdutoVariacao produtoVariacao;

    // Quantidade do produto no item da venda
    @NotNull(message = "A quantidade é obrigatória.")
    @Positive(message = "A quantidade deve ser maior que zero.")
    @Column(nullable = false)
    private Integer quantidade;

    // Preço unitário do produto no item da venda
    @NotNull(message = "O preço unitário é obrigatório.")
    @DecimalMin(value = "0.01", message = "O preço unitário deve ser maior que zero.")
    @Digits(integer = 10, fraction = 2)
    @Column(name = "preco_unitario", nullable = false, precision = 12, scale = 2)
    private BigDecimal precoUnitario;

    // Subtotal do item da venda (quantidade * preço unitário)
    @NotNull(message = "O subtotal é obrigatório.")
    @DecimalMin(value = "0.01", message = "O subtotal deve ser maior que zero.")
    @Digits(integer = 10, fraction = 2)
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal subtotal;
}
