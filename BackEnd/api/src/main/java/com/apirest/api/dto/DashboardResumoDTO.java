package com.apirest.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardResumoDTO {
    private BigDecimal totalDinheiro;
    private BigDecimal totalPix;
    private BigDecimal totalDebito;
    private BigDecimal totalCredito;
    private BigDecimal totalCrediario;
    private BigDecimal totalGeral;
    private BigDecimal totalQuebra;
}