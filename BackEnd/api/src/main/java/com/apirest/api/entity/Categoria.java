package com.apirest.api.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "categorias", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"nome"})
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Categoria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_categoria")
    private Long idCategoria;

    // armazenamos em MAIÚSCULAS (normalização feita no serviço)
    @Column(nullable = false, unique = true)
    private String nome;
}