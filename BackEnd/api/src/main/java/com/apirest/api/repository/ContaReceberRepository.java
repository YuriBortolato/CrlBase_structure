package com.apirest.api.repository;
import com.apirest.api.entity.Cliente;
import com.apirest.api.entity.ContaReceber;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface ContaReceberRepository extends JpaRepository<ContaReceber, Long> {
    // Repositório para gerenciar entidades ContaReceber
    List<ContaReceber> findByCliente(Cliente cliente);
    @Query("SELECT COALESCE(SUM(c.valorTotal), 0) FROM ContaReceber c WHERE c.cliente.id = :idCliente AND c.status = 'ABERTA'")
    BigDecimal somarDividaAberta(@Param("idCliente") Long idCliente);
}