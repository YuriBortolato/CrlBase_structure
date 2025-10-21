package com.apirest.api.dto;

import lombok.*;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VendaItemResponseDTO {
    private Long idProduto;
    private String nomeProduto;
    private Integer quantidade;
    private BigDecimal precoUnitario;
    private BigDecimal subtotal;
}