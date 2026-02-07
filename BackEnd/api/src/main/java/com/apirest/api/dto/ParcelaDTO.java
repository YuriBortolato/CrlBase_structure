package com.apirest.api.dto;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
public class ParcelaDTO {
    private Integer numero; // 1, 2, 3...
    private BigDecimal valorOriginal;
    private BigDecimal valorPago;
    private LocalDate dataVencimento;
    private String status; // PENDENTE, PAGA, ATRASADA
}