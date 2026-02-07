package com.apirest.api.dto;

import com.apirest.api.entity.MetodoPagamento;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VendaDTO {

    @NotNull(message = "idFuncionario é obrigatório")
    private Long idFuncionario;

    @NotNull(message = "idCliente é obrigatório")
    private Long idCliente;

    @NotEmpty(message = "A venda deve conter ao menos um item")
    private List<@Valid VendaItemDTO> itens;

    @NotNull(message = "O método de pagamento é obrigatório")
    private MetodoPagamento metodoPagamento;

    @Size(max = 255, message = "Observações não podem ter mais que 255 caracteres")
    private String observacoes;

    // Cupom de desconto opcional
    private String codigoCupom;

    // Desconto manual opcional
    @DecimalMin(value = "0.00", message = "O desconto manual não pode ser negativo")
    private BigDecimal descontoManual;

    // Número de parcelas para pagamento parcelado (usado apenas se metodoPagamento == CREDIARIO)
    @Min(value = 1, message = "O número de parcelas deve ser pelo menos 1")
    private Integer numeroParcelas;

    // Valor pago pelo cliente
    @DecimalMin(value = "0.00", message = "O valor pago não pode ser negativo")
    private BigDecimal valorPagoCliente; // Valor pago fisico pelo cliente
}