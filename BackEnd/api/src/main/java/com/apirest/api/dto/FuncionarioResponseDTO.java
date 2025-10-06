package com.apirest.api.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FuncionarioResponseDTO {
    private Long id;
    private String email;
    private String nome;
}
