package com.apirest.api.dto;

import com.apirest.api.entity.Cargo;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.*;
import lombok.*;
import org.hibernate.validator.constraints.br.CPF;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FuncionarioDTO {

    @NotNull(message = "O ID da unidade é obrigatório.")
    private Long idUnidade;

    @NotNull(message = "O cargo é obrigatório.")
    private Cargo cargo;

    @NotBlank(message = "O nome é obrigatório.")
    @Pattern(regexp = "^[A-Za-zÀ-ÿ\\s]{5,255}$", message = "O nome deve conter apenas letras e ter pelo menos 5 caracteres")
    private String nomeCompleto;

    @NotBlank(message = "O nome de registro é obrigatório.")
    @Pattern(regexp = "^[A-Za-zÀ-ÿ\\s]{3,50}$", message = "O nome de registro deve conter apenas letras e ter entre 3 e 50 caracteres.")
    private String nomeRegistro;

    @NotBlank(message = "O CPF é obrigatório.")
    @CPF(message = "CPF inválido")
    private String cpf;

    @NotBlank(message = "O e-mail é obrigatório.")
    @Email(message = "O e-mail deve ser válido.")
    private String email;

    @NotNull(message = "A data de nascimento é obrigatória.")
    @Past(message = "A data de nascimento deve ser no passado.")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd-MM-yyyy")
    private LocalDate dataNascimento;

    @NotBlank(message = "O telefone é obrigatório.")
    @Pattern(regexp = "^\\+?\\d{10,15}$", message = "O telefone deve conter apenas números e ter entre 10 e 15 dígitos.")
    private String telefone;

    @NotBlank(message = "O login é obrigatório.")
    @Pattern(regexp = "^[A-Za-z0-9]{4,20}$", message = "O login deve ter entre 4 e 20 caracteres e conter apenas letras e números")
    private String login;

    @NotBlank(message = "A senha é obrigatória.")
    @Size(min = 6, message = "Senha deve ter no mínimo 6 caracteres")
    private String senha;
    public void setEmail(String email) {
        if (email != null) {
            this.email = email.toLowerCase();
        } else {
            this.email = null;
        }
    }
}
