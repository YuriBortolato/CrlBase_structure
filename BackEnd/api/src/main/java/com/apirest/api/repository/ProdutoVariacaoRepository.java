package com.apirest.api.repository;

import com.apirest.api.entity.ProdutoVariacao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProdutoVariacaoRepository extends JpaRepository<ProdutoVariacao, Long> {
    Optional<ProdutoVariacao> findByCodigoBarras(String ean);
    Optional<ProdutoVariacao> findBySku(String sku);
}