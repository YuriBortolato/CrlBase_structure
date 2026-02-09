package com.apirest.api.repository;

import com.apirest.api.entity.Parcela;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;

@Repository
public interface ParcelaRepository extends JpaRepository<Parcela, Long> {

    // Verifica se existe alguma parcela vencida e não paga para este cliente
    @Query("""
       SELECT COUNT(p) > 0 
       FROM Parcela p 
       JOIN p.contaReceber c 
       WHERE c.cliente.id = :idCliente 
       AND p.status = 'PENDENTE' 
       AND p.dataVencimento < :hoje
    """)
    boolean existeParcelaAtrasada(@Param("idCliente") Long idCliente, @Param("hoje") LocalDate hoje);
}