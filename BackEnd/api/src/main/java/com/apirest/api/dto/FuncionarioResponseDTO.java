package com.apirest.api.dto;

import com.apirest.api.entity.Cargo;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FuncionarioResponseDTO {
    private Long idFuncionario;
    private Cargo cargo;
    private String nomeCompleto;
    private String cpf;
    private String email;
    private String telefone;
    private String login;

    private boolean ativo;
}
