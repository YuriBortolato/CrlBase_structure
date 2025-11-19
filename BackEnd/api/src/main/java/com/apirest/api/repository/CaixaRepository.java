package com.apirest.api.repository;

import com.apirest.api.entity.Caixa;
import com.apirest.api.entity.Funcionario;
import com.apirest.api.entity.StatusCaixa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface CaixaRepository extends JpaRepository<Caixa, Long> {

    // Verifica se o funcionário já possui um caixa aberto
    boolean existsByFuncionarioAndStatus(Funcionario funcionario, StatusCaixa status);

    // Busca o caixa aberto de um funcionário específico
    Optional<Caixa> findByFuncionarioAndStatus(Funcionario funcionario, StatusCaixa status);

    // Busca caixas com filtros opcionais
    @Query("SELECT c FROM Caixa c WHERE " +
            "(:idFuncionario IS NULL OR c.funcionario.idFuncionario = :idFuncionario) AND " +
            "(:status IS NULL OR c.status = :status) AND " +
            "(c.dataAbertura BETWEEN :dataInicio AND :dataFim)")
    List<Caixa> findByFiltros(
            @Param("idFuncionario") Long idFuncionario,
            @Param("status") StatusCaixa status,
            @Param("dataInicio") LocalDateTime dataInicio,
            @Param("dataFim") LocalDateTime dataFim
    );
    // Soma o total conferido em caixas fechados por um funcionário em um período específico
    @Query("SELECT c FROM Caixa c WHERE c.funcionario.idFuncionario = :idFuncionario " +
            "AND c.status = 'FECHADO' " +
            "AND c.dataFechamento BETWEEN :inicio AND :fim")
    List<Caixa> findCaixasFechadosPorPeriodo(
            @Param("idFuncionario") Long idFuncionario,
            @Param("inicio") LocalDateTime inicio,
            @Param("fim") LocalDateTime fim
    );

    // Busca todos os caixas fechados em um período específico
    @Query("SELECT c FROM Caixa c WHERE c.status = 'FECHADO' " +
            "AND c.dataFechamento BETWEEN :inicio AND :fim")
    List<Caixa> findAllCaixasFechadosPorPeriodo(
            @Param("inicio") LocalDateTime inicio,
            @Param("fim") LocalDateTime fim
    );
}