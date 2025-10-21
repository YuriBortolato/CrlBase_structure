package com.apirest.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.*;

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

    @NotBlank(message = "O método de pagamento é obrigatório")
    @Size(max = 50, message = "Método de pagamento deve ter no máximo 50 caracteres")
    private String metodoPagamento;

    @Size(max = 255, message = "Observações não podem ter mais que 255 caracteres")
    private String observacoes;
}