package com.apirest.api.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "caixa_movimentacoes")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CaixaMovimentacao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "id_caixa", nullable = false)
    private Caixa caixa;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoMovimentacao tipo; // SANGRIA, SUPRIMENTO

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal valor;

    @Column(nullable = false)
    private String motivo;

    @Column(name = "data_hora")
    private LocalDateTime dataHora;

    @ManyToOne
    @JoinColumn(name = "id_usuario_autorizador")
    private Funcionario usuarioAutorizador;

    public enum TipoMovimentacao {
        SANGRIA, SUPRIMENTO, ENTRADA
    }
}