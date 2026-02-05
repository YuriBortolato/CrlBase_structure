package com.apirest.api.repository;

import com.apirest.api.entity.VendaPagamento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface VendaPagamentoRepository extends JpaRepository<VendaPagamento, Long> {
    // Repositório para gerenciar entidades VendaPagamento
}