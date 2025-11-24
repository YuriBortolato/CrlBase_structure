package com.apirest.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponseDTO {
    private Long id;
    private String nome;
    private String tipoUsuario; // "FUNCIONARIO" ou "CLIENTE"
    private String cargo;       // Se for cliente, sera nulo ou "CLIENTE"
    private String token;       // JWT futuramente
}