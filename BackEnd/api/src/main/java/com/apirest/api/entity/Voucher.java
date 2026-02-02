package com.apirest.api.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "marketing_vouchers")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Voucher {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String codigo; // Ex: PROMO10

    @Enumerated(EnumType.STRING)
    private TipoDesconto tipo; // PERCENTUAL ou FIXO

    private BigDecimal valor; // 10.00; 10% ou R$ 10,00

    private Integer quantidadeDisponivel;
    private LocalDateTime validadeInicio;
    private LocalDateTime validadeFim;
    private boolean ativo;
    private boolean acumulativo; // Pode ser usado com outras promoções?

    public enum TipoDesconto { PERCENTUAL, FIXO }
}