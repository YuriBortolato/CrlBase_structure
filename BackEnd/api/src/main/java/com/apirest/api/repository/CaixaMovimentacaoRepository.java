package com.apirest.api.repository;

import com.apirest.api.entity.Caixa;
import com.apirest.api.entity.CaixaMovimentacao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;

@Repository
public interface CaixaMovimentacaoRepository extends JpaRepository<CaixaMovimentacao, Long> {

    @Query("SELECT SUM(m.valor) FROM CaixaMovimentacao m WHERE m.caixa = :caixa AND m.tipo = :tipo")
    BigDecimal somarPorTipo(@Param("caixa") Caixa caixa, @Param("tipo") CaixaMovimentacao.TipoMovimentacao tipo);
}