package com.apirest.api.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

@Entity
@Table(name = "funcionarios")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Funcionario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idFuncionario;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Cargo cargo;

    @NotBlank(message = "O nome é obrigatório.")
    @Column(nullable = false)
    private String nomeCompleto;

    @NotBlank(message = "O CPF é obrigatório.")
    @Column(nullable = false, unique = true, length = 11)
    private String cpf;

    @Email(message = "O e-mail deve ser válido.")
    @NotBlank(message = "O e-mail é obrigatório.")
    @Column(nullable = false, unique = true)
    private String email;

    @NotBlank(message = "O telefone é obrigatório.")
    @Column(nullable = false)
    private String telefone;

    @NotBlank(message = "O login é obrigatório.")
    @Column(nullable = false, unique = true)
    private String login;

    @NotBlank(message = "A senha é obrigatória.")
    @Column(nullable = false)
    private String senhaCriptografada;
}