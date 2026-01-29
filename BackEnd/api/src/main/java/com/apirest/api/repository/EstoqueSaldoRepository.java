package com.apirest.api.repository;

import com.apirest.api.entity.EstoqueSaldo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface EstoqueSaldoRepository extends JpaRepository<EstoqueSaldo, Long> {
    Optional<EstoqueSaldo> findByUnidadeIdAndProdutoVariacaoId(Long unidadeId, Long variacaoId);
}