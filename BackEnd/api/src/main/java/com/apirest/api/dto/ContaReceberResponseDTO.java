package com.apirest.api.dto;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class ContaReceberResponseDTO {
    private Long idConta;
    private Long idVendaOrigem;
    private String nomeCliente;
    private String cpfCliente;

    private BigDecimal valorTotal;
    private Integer quantidadeParcelas;
    private String statusConta; // ABERTA, QUITADA
    private LocalDateTime dataCriacao;

    private List<ParcelaDTO> parcelas;
}