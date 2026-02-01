package com.apirest.api.repository;

import com.apirest.api.entity.EstoqueSaldo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface EstoqueSaldoRepository extends JpaRepository<EstoqueSaldo, Long> {

    @Query("SELECT e FROM EstoqueSaldo e WHERE e.unidade.idUnidade = :unidadeId AND e.produtoVariacao.id = :variacaoId")
    Optional<EstoqueSaldo> findByUnidadeIdAndProdutoVariacaoId(@Param("unidadeId") Long unidadeId, @Param("variacaoId") Long variacaoId);
}