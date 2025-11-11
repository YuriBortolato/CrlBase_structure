package com.apirest.api.entity;

import com.fasterxml.jackson.annotation.JsonCreator;

import java.util.Arrays;

public enum StatusVenda {
    REALIZADA,
    CANCELADA;


    @JsonCreator
    public static StatusVenda fromString(String value) {
        // verifica se o texto foi nulo ou vazio
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("O status da venda não pode ser nulo ou vazio.");
        }

        // itera sobre os valores do enum para encontrar uma correspondência
        for (StatusVenda status : StatusVenda.values()) {
            // compara ignorando maiúsculas e minúsculas
            if (status.name().equalsIgnoreCase(value.trim())) {
                // retorna o valor do enum correspondente
                return status;
            }
        }

        // lança exceção se nenhum valor correspondente for encontrado
        throw new IllegalArgumentException("Status de venda inválido: '" + value + "'. Os valores aceitos são: " +
                Arrays.toString(StatusVenda.values()));
    }
}
