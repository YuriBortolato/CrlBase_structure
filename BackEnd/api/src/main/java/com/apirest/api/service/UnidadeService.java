package com.apirest.api.service;

import com.apirest.api.entity.TipoDocumento;
import com.apirest.api.entity.Unidade;
import com.apirest.api.repository.UnidadeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UnidadeService {

    private final UnidadeRepository repository;

    @Transactional
    public Unidade criarUnidade(Unidade unidade) {
        long totalLojas = repository.count();

        // Regra: A partir da segunda unidade, o documento deve ser CNPJ
        if (totalLojas > 0 && unidade.getTipoDocumento() == TipoDocumento.CPF) {
            throw new RuntimeException("Regra de Expansão Violada: A partir da segunda unidade, é obrigatório o uso de CNPJ.");
        }

        if (repository.existsByDocumentoNumero(unidade.getDocumentoNumero())) {
            throw new RuntimeException("Este documento já está cadastrado em outra unidade.");
        }

        return repository.save(unidade);
    }

    public long getQuantidadeUnidades() {
        return repository.count();
    }
}