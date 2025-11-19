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
    @Column(nullable = false)
    private StatusCaixa status;

    // --- Valores informados pelo funcionario na abertura ---
    @Column(name = "saldo_inicial", nullable = false)
    private BigDecimal saldoInicial;

    // --- Valores informados pelo funcionario no fechamento ---
    private BigDecimal conferidoDinheiro;
    private BigDecimal conferidoPix;
    private BigDecimal conferidoDebito;
    private BigDecimal conferidoCredito;
    private BigDecimal conferidoCrediario;

    // --- Valores calculados pelo sistema ---
    private BigDecimal sistemaTotalVendas; // Total de vendas registradas no caixa
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