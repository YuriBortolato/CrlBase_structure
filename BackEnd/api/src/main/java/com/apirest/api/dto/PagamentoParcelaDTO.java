package com.apirest.api.dto;

import com.apirest.api.entity.MetodoPagamento;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class PagamentoParcelaDTO {
    @NotNull(message = "O ID da parcela é obrigatório")
    private Long idParcela;

    @NotNull(message = "O ID do funcionário que está recebendo é obrigatório")
    private Long idFuncionarioRecebedor;

    @NotNull(message = "O valor pago é obrigatório")
    @Positive(message = "O valor deve ser maior que zero")
    private BigDecimal valorPago;

    @NotNull(message = "O método de pagamento é obrigatório")
    private MetodoPagamento metodoPagamento; // DINHEIRO, PIX, DEBITO
}