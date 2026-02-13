package com.apirest.api.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import org.hibernate.validator.constraints.br.CPF;

import java.time.LocalDate;

@Entity
@Table(name = "clientes")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Cliente {

    //
    @ManyToOne
    @JoinColumn(name = "unidade_origem_id")
    private Unidade unidadeOrigem;

    // Perfil de Acesso do App
    @ManyToOne
    @JoinColumn(name = "perfil_acesso_id")
    private PerfilAcesso perfilAcesso;

    // Elo de Sincronização. Se preenchido, este cliente É um funcionário.
    @OneToOne
    @JoinColumn(name = "funcionario_origem_id", unique = true, nullable = true)
    private Funcionario funcionarioOrigem;

    // Campos Financeiros
    @Column(name = "limite_credito", precision = 12, scale = 2)
    private java.math.BigDecimal limiteCredito; // Quanto ele pode gastar fiado

    @Builder.Default
    private boolean bloqueadoFiado = false; // Se deve bloquear compras a prazo

    // Campos Jurídicos
    private String assinaturaDigitalUrl;
    private Integer termoAceitoVersaoId;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_cliente")
    private Long idCliente;

    @NotBlank(message = "O nome completo é obrigatório.")
    @Pattern(regexp = "^[A-Za-zÀ-ÿ\\s]{3,255}$", message = "O nome deve conter apenas letras e ter entre 3 e 255 caracteres.")
    @Column(name = "nome_completo", nullable = false)
    private String nomeCompleto;


    @NotBlank(message = "O e-mail é obrigatório.")
    @Email(message = "E-mail inválido.")
    @Column(nullable = false, unique = true)
    private String email;


    @NotBlank(message = "O CPF é obrigatório.")
    @CPF(message = "CPF inválido")
    @Column(nullable = false, unique = true, length = 11)
    private String cpf;

    @Past(message = "A data de nascimento deve ser no passado.")
    @NotNull(message = "A data de nascimento é obrigatória.")
    @Column(name = "data_nascimento", nullable = false)
    private LocalDate dataNascimento;

    @Pattern(regexp = "^\\+?\\d{10,15}$", message = "O telefone deve conter apenas números e ter entre 10 e 15 dígitos.")
    @Column(length = 15) // Ajustado o tamanho para 15 caracteres
    private String telefone;

    @NotBlank(message = "O login é obrigatório.")
    @Pattern(regexp = "^[a-zA-Z0-9]{4,20}$", message = "O login deve conter apenas letras e números, com 4 a 20 caracteres.")
    @Column(nullable = false, unique = true)
    private String login;

    @NotBlank(message = "A senha é obrigatória.")
    @Column(nullable = false)
    private String senhaCriptografada;

    @Builder.Default
    @Column(nullable = false)
    private boolean ativo = true;
}
