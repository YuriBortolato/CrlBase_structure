package com.apirest.api.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Entity
@Table(name = "venda_descontos")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VendaDesconto {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "venda_id")
    private Venda venda;

    private String origem; // "VOUCHER", "MANUAL"
    private String codigoReferencia; // "NATAL10" ou "Erro no sistema"
    private BigDecimal valorDescontoAplicado;
}