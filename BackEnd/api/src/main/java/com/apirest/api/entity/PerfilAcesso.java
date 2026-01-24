package com.apirest.api.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "perfil_acesso")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PerfilAcesso {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String nome; // Nome do perfil de acesso

    @Enumerated(EnumType.STRING)
    private TipoPerfil tipo; // Enum: INTERNO (Funcionario) ou EXTERNO (Cliente)

    private String descricao;

    // Outros campos relevantes serão add no futuro
}