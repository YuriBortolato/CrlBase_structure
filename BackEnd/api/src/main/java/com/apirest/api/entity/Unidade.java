package com.apirest.api.entity;
    import jakarta.persistence.*;
    import jakarta.validation.constraints.NotBlank;
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

    // Apenas para CNPJ
    private String razaoSocial;
    private String inscricaoEstadual;
}

