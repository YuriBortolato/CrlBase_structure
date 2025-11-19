package com.apirest.api.entity;

import com.fasterxml.jackson.annotation.JsonCreator;

import java.util.Arrays;

public enum MetodoPagamento {
    PIX,
    DEBITO,
    CREDITO,
    DINHEIRO,
    CREDIARIO;

    @JsonCreator
    public static MetodoPagamento fromString(String value) {
        // verifica se o texto foi nulo ou vazio
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("O método de pagamento não pode ser nulo ou vazio.");
        }
        String upperValue = value.trim().toUpperCase().replaceAll(" ", "_");

        // itera sobre os valores do enum para encontrar uma correspondência
        for (MetodoPagamento metodo : MetodoPagamento.values()) {
            // compara ignorando maiúsculas e minúsculas
            if (metodo.name().equalsIgnoreCase(value.trim())) {
                // retorna o valor do enum correspondente
                return metodo;
            }
        }

        // lança exceção se nenhum valor correspondente for encontrado
        throw new IllegalArgumentException("Método de pagamento inválido: '" + value + "'. Os valores aceitos são: " +
                Arrays.toString(MetodoPagamento.values()));
    }
}
