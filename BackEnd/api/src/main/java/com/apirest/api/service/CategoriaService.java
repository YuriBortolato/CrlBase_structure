package com.apirest.api.service;

import com.apirest.api.entity.Categoria;
import com.apirest.api.repository.CategoriaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CategoriaService {

    private final CategoriaRepository categoriaRepository;

    // Metodo para encontrar ou criar uma categoria com nome normalizado
    @Transactional
    public Categoria findOrCreateByNameNormalize(String nome) {
        if (nome == null || nome.trim().isEmpty()) {
            throw new IllegalArgumentException("O nome da categoria não pode ser nulo ou vazio.");
        }
        // Normaliza o nome: remove espaços e converte para maiúsculas
        String upper = nome.trim().toUpperCase();
        return categoriaRepository.findByNome(upper)
                .orElseGet(() -> categoriaRepository.save(Categoria.builder().nome(upper).build()));
    }
}