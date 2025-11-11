package com.apirest.api.dto;

import com.apirest.api.entity.MetodoPagamento;
import com.apirest.api.entity.StatusVenda;
import com.fasterxml.jackson.annotation.JsonFormat;
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
    @JsonFormat(pattern = "dd-MM-yyyy HH:mm:ss")
    private LocalDateTime dataVenda;
    private MetodoPagamento metodoPagamento;
    private StatusVenda statusVenda;
    private String observacoes;
}