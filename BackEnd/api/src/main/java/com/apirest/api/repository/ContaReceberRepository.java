package com.apirest.api.repository;
import com.apirest.api.entity.Cliente;
import com.apirest.api.entity.ContaReceber;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ContaReceberRepository extends JpaRepository<ContaReceber, Long> {
    // Repositório para gerenciar entidades ContaReceber
    List<ContaReceber> findByCliente(Cliente cliente);
}