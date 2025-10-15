package com.apirest.api.dto;

import jakarta.validation.constraints.*;
import lombok.*;
import org.hibernate.validator.constraints.br.CPF;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ClienteDTO {

    @NotBlank(message = "O nome é obrigatório.")
    @Pattern(regexp = "^[A-Za-zÀ-ÿ\\s]{3,50}$", message = "O nome deve conter apenas letras e ter entre 3 e 50 caracteres.")
    private String nomeCompleto;

    @NotBlank(message = "O e-mail é obrigatório.")
    @Email(message = "E-mail inválido.")
    private String email;

    @NotBlank(message = "O CPF é obrigatório.")
    @CPF(message = "CPF inválido")
    private String cpf;

    @NotBlank(message = "O telefone é obrigatório.")
    @Pattern(regexp = "^\\d{11}$", message = "Telefone deve ter 11 dígitos numéricos")
    private String telefone;

    @NotBlank(message = "O login é obrigatório.")
    @Pattern(regexp = "^[A-Za-z0-9]{4,20}$", message = "O login deve ter entre 4 e 20 caracteres e conter apenas letras e números")
    private String login;

    @NotBlank(message = "Senha é obrigatória")
    @Size(min = 6, message = "Senha deve ter no mínimo 6 caracteres")
    private String senha;
}
