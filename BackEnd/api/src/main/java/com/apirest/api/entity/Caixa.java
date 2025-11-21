package com.apirest.api.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

@Entity
@Table(name = "caixas")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Caixa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idCaixa;

    @ManyToOne(optional = false)
    @JoinColumn(name = "id_funcionario", nullable = false)
    private Funcionario funcionario;

    @Column(nullable = false)
    private LocalDateTime dataAbertura;

    private LocalDateTime dataFechamento;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20) // Tamanho suficiente para armazenar os valores do enum
    private StatusCaixa status;

    // --- Valores informados pelo funcionario na abertura ---
    @Column(name = "saldo_inicial", nullable = false, precision = 12, scale = 2)
    private BigDecimal saldoInicial;

    // --- Valores informados pelo funcionario no fechamento ---
    @Column(precision = 12, scale = 2)
    private BigDecimal conferidoDinheiro;

    @Column(precision = 12, scale = 2)
    private BigDecimal conferidoPix;

    @Column(precision = 12, scale = 2)
    private BigDecimal conferidoDebito;

    @Column(precision = 12, scale = 2)
    private BigDecimal conferidoCredito;

    @Column(precision = 12, scale = 2)
    private BigDecimal conferidoCrediario;

    // --- Valores calculados pelo sistema ---
    @Column(precision = 12, scale = 2)
    private BigDecimal sistemaTotalVendas; // Total de vendas registradas no caixa

    @Column(precision = 12, scale = 2)
    private BigDecimal quebraDeCaixa;      // Diferença entre o que foi conferido e o que o sistema calculou

    @Column(length = 500)
    private String observacoes;

    // Relação com vendas realizadas durante o período do caixa
    @OneToMany(mappedBy = "caixa", fetch = FetchType.LAZY)
    private List<Venda> vendas;

    @PrePersist
    public void prePersist() {
        if (dataAbertura == null) {
            dataAbertura = LocalDateTime.now(ZoneId.of("America/Sao_Paulo"));
        }
        if (status == null) {
            status = StatusCaixa.ABERTO;
        }
    }
}