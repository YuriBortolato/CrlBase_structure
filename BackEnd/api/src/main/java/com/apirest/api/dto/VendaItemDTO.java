package com.apirest.api.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VendaItemDTO {

    @NotNull(message = "idProduto é obrigatório")
    private Long idProduto;

    @NotNull(message = "Quantidade é obrigatória")
    @Min(value = 1, message = "Quantidade deve ser pelo menos 1")
    private Integer quantidade;
}
