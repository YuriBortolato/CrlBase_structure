package com.apirest.api.entity;

import java.util.Arrays;

public enum Cargo {
    ADMIN,
    DONO,
    GERENTE,
    LIDER_VENDA,
    RECEPCIONISTA,
    RECEPCIONISTA_TESTE;

    public static Cargo fromString(String value) {
        // verifica se o texto foi nulo ou vazio
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("O nome do cargo não pode ser nulo ou vazio.");
        }

        // itera sobre os valores do enum para encontrar uma correspondência
        for (Cargo cargoEnum : Cargo.values()) {
            // compara ignorando maiúsculas e minúsculas
            if (cargoEnum.name().equalsIgnoreCase(value.trim())) {
                // retorna o valor do enum correspondente
                return cargoEnum;
            }
        }

        // lança exceção se nenhum valor correspondente for encontrado
        throw new IllegalArgumentException("Cargo inválido: '" + value + "'. Os valores aceitos são: " +
                Arrays.toString(Cargo.values()));
    }
}
