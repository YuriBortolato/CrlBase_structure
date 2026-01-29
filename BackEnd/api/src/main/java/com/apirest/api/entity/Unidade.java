package com.apirest.api.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Entity
@Table(name = "unidade")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Unidade {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idUnidade;

    // Associação com Grupo Econômico
    @NotNull(message = "ID do Grupo Econômico é obrigatório")
    @Column(name = "grupo_economico_id", nullable = false)
    private Long grupoEconomicoId;

    @NotBlank(message = "Nome fantasia é obrigatório")
    private String nomeFantasia;

    @NotBlank(message = "Documento (CPF/CNPJ) é obrigatório")
    @Column(unique = true)
    private String documentoNumero;

    @Enumerated(EnumType.STRING)
    private TipoDocumento tipoDocumento; // CPF ou CNPJ

    private String telefone;
    private String emailContato;

    // Endereço
    private String cep;
    private String logradouro;
    private String numero;
    private String bairro;
    private String cidade;
    private String uf;

    //opcional
    private String complemento;
    private String razaoSocial;
    private String inscricaoEstadual;
}

