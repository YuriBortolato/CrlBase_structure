package com.apirest.api.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VendaResponseDTO {
    private Long idVenda;
    private Long idFuncionario;
    private String nomeFuncionario;
    private Long idCliente;
    private String nomeCliente;
    private List<VendaItemResponseDTO> itens;
    private BigDecimal valorTotal;
    private LocalDateTime dataVenda;
    private String metodoPagamento;
    private String observacoes;
}