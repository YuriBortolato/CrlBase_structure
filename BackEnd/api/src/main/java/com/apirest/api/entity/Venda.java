package com.apirest.api.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.time.ZoneId;

@Entity
@Table(name = "venda")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Venda {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_venda")
    private Long idVenda;

    // Funcionário responsável pela venda
    @ManyToOne(optional = false)
    @JoinColumn(name = "id_funcionario", nullable = false)
    @NotNull(message = "O funcionário responsável pela venda é obrigatório.")
    private Funcionario funcionario;

    // Cliente associado à venda
    @ManyToOne(optional = false)
    @JoinColumn(name = "id_cliente", nullable = false)
    @NotNull(message = "O cliente da venda é obrigatório.")
    private Cliente cliente;

    // Caixa associado à venda
    @ManyToOne(optional = false)
    @JoinColumn(name = "id_caixa", nullable = false)
    private Caixa caixa;

    // Lista de itens da venda
    @OneToMany(mappedBy = "venda", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @NotEmpty(message = "A venda deve conter ao menos um item.")
    private List<VendaItem> itens;

    // Valor total da venda
    @DecimalMin(value = "0.01", inclusive = true, message = "O valor total deve ser maior que zero.")
    @Digits(integer = 10, fraction = 2, message = "O valor total deve ter no máximo 10 dígitos inteiros e 2 decimais.")
    @Column(name = "valor_total", nullable = false, precision = 12, scale = 2)
    private BigDecimal valorTotal;

    // Data e hora da venda
    @Column(name = "data_venda", nullable = false, updatable = false)
    private LocalDateTime dataVenda;

    // Metodo de pagamento (Enum)
    @Enumerated(EnumType.STRING)
    @Column(name = "metodo_pagamento", nullable = false, length = 50)
    @NotNull(message = "O método de pagamento é obrigatório.")
    private MetodoPagamento metodoPagamento;

    // Status da venda (Enum)
    @Enumerated(EnumType.STRING)
    @Column(name = "status_venda", nullable = false, length = 20)
    @Builder.Default
    private StatusVenda statusVenda = StatusVenda.REALIZADA;

    // Observações adicionais sobre a venda
    @Size(max = 255, message = "Observações não podem ter mais de 255 caracteres.")
    @Column(length = 255)
    private String observacoes;

    // Define a data da venda antes de persistir
    @PrePersist
    public void prePersist() {
        if (dataVenda == null) {
            dataVenda = LocalDateTime.now(ZoneId.of("America/Sao_Paulo"));
        }
    }
}
