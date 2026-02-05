package com.apirest.api.dto;

import jakarta.validation.constraints.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class VoucherDTO {

    @NotNull(message = "Quem está criando o cupom? (ID Funcionario)")
    private Long idFuncionarioCriador;

    @NotBlank(message = "O código do cupom é obrigatório")
    @Size(min = 3, max = 20)
    @Pattern(regexp = "^[A-Z0-9]+$", message = "O código deve conter apenas letras maiúsculas e números.")
    private String codigo;

    @NotNull(message = "Tipo de desconto (PERCENTUAL ou FIXO) é obrigatório")
    private String tipo;

    @NotNull
    @DecimalMin("0.01")
    private BigDecimal valor;

    @NotNull
    @Min(1)
    private Integer quantidadeDisponivel;

    @NotNull
    private LocalDateTime validadeFim;

    private boolean acumulativo;
}