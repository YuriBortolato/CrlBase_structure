package com.apirest.api.dto;

import com.apirest.api.entity.Cargo;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.br.CPF;

@Data
@NoArgsConstructor
public class FuncionarioPatchDTO {

    private Cargo cargo;

    @Pattern(regexp = "^[A-Za-zÀ-ÿ\\s]{5,255}$", message = "O nome deve conter apenas letras e ter pelo menos 5 caracteres")
    private String nomeCompleto;

    @CPF(message = "CPF inválido")
    private String cpf;

    @Email(message = "O e-mail deve ser válido.")
    private String email;

    @Pattern(regexp = "^\\d{11}$", message = "Telefone deve ter 11 dígitos numéricos")
    private String telefone;

    @Pattern(regexp = "^[A-Za-z0-9]{4,20}$", message = "O login deve ter entre 4 e 20 caracteres e conter apenas letras e números")
    private String login;

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