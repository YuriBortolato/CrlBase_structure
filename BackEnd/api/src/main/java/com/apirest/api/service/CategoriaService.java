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

    /**
     * Retorna a categoria existente (buscando por nome em MAIÚSCULAS) ou cria uma nova com nome em MAIÚSCULAS.
     */
    @Transactional
    public Categoria findOrCreateByNameNormalize(String nome) {
        String upper = nome.trim().toUpperCase();
        return categoriaRepository.findByNome(upper)
                .orElseGet(() -> categoriaRepository.save(Categoria.builder().nome(upper).build()));
    }
}