package com.apirest.api.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "contas_receber")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ContaReceber {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne // Uma venda gera uma conta a receber
    @JoinColumn(name = "id_venda", nullable = false)
    private Venda venda;

    @ManyToOne
    @JoinColumn(name = "id_cliente", nullable = false)
    private Cliente cliente;

    @Column(name = "valor_total", nullable = false)
    private BigDecimal valorTotal;

    @Column(name = "quantidade_parcelas", nullable = false)
    private Integer quantidadeParcelas;

    @Enumerated(EnumType.STRING)
    private StatusConta status; // ABERTA, QUITADA

    @Column(name = "data_criacao")
    private LocalDateTime dataCriacao;

    // Relacionamento inverso para acessar as parcelas associadas a esta conta a receber
    @OneToMany(mappedBy = "contaReceber", cascade = CascadeType.ALL)
    private List<Parcela> parcelas;

    public enum StatusConta {
        ABERTA, QUITADA
    }
}