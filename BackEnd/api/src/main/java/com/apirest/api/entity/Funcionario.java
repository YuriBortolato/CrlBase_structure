package com.apirest.api.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import org.hibernate.validator.constraints.br.CPF;

import java.time.LocalDate;

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

    @ManyToOne(optional = false)
    @JoinColumn(name = "id_unidade", nullable = false)
    private Unidade unidade;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Cargo cargo;

    @NotBlank(message = "O nome é obrigatório.")
    @Pattern(regexp = "^[A-Za-zÀ-ÿ\\s]{5,255}$", message = "O nome deve conter apenas letras e ter pelo menos 5 caracteres")
    @Column(nullable = false)
    private String nomeCompleto;

    @NotBlank(message = "O nome de registro é obrigatório.")
    @Pattern(regexp = "^[A-Za-zÀ-ÿ\\s]{3,50}$", message = "O nome de registro deve conter apenas letras e ter entre 3 e 50 caracteres.")
    @Column(nullable = false, name = "nome_registro")
    private String nomeRegistro;

    @NotBlank(message = "O CPF é obrigatório.")
    @CPF(message = "CPF inválido")
    @Column(nullable = false, unique = true, length = 11)
    private String cpf;

    @Email(message = "O e-mail deve ser válido.")
    @NotBlank(message = "O e-mail é obrigatório.")
    @Column(nullable = false, unique = true)
    private String email;

    @NotNull(message = "A data de nascimento é obrigatória.")
    @Past(message = "A data de nascimento deve ser no passado.")
    @Column(nullable = false, name = "data_nascimento")
    private LocalDate dataNascimento;

    @NotBlank(message = "O telefone é obrigatório.")
    @Pattern(regexp = "^\\+?\\d{10,15}$", message = "O telefone deve conter apenas números e ter entre 10 e 15 dígitos.")
    @Column(nullable = false, length = 15)
    private String telefone;

    @NotBlank(message = "O login é obrigatório.")
    @Pattern(regexp = "^[A-Za-z0-9]{4,20}$", message = "O login deve ter entre 4 e 20 caracteres e conter apenas letras e números")
    @Column(nullable = false, unique = true)
    private String login;

    @NotBlank(message = "A senha é obrigatória.")
    @Column(nullable = false)
    private String senhaCriptografada;

    @Builder.Default
    @Column(nullable = false)
    private boolean ativo = true;
}