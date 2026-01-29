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
        // Conta quantas unidades já existem para o mesmo grupo econômico
        long totalLojasDoGrupo = repository.countByGrupoEconomicoId(unidade.getGrupoEconomicoId());

        // Aplica a regra de expansão: a partir da segunda unidade, o documento deve ser CNPJ
        if (totalLojasDoGrupo > 0 && unidade.getTipoDocumento() == TipoDocumento.CPF) {
            throw new RuntimeException("Regra de Expansão Violada: A partir da segunda unidade, é obrigatório o uso de CNPJ.");
        }

        if (repository.existsByDocumentoNumero(unidade.getDocumentoNumero())) {
            throw new RuntimeException("Este documento já está cadastrado.");
        }

        return repository.save(unidade);
    }
}