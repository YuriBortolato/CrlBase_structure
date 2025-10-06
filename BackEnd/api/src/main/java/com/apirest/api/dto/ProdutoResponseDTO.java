package com.apirest.api.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductResponseDTO {
    private Long id;
    private String nome;
    private Double preco;
    private String descricao;
}
