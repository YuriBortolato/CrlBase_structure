package com.apirest.api.dto;

import com.apirest.api.entity.Cargo;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FuncionarioResponseDTO {
    private Long idFuncionario;
    private Cargo cargo;
    private String nomeCompleto;
    private String nomeRegistro;
    private String cpf;
    private String email;
    private String telefone;
    private String login;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd-MM-yyyy")
    private LocalDate dataNascimento;

    private boolean ativo;
}
