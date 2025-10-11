package com.apirest.api.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ClienteResponseDTO {
    private Long idCliente;
    private String nomeCompleto;
    private String email;
    private String cpf;
    private String telefone;
    private String login;
}
