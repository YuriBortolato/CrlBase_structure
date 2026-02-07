package com.apirest.api.repository;
import com.apirest.api.entity.ContaReceber;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ContaReceberRepository extends JpaRepository<ContaReceber, Long> {
    // Repositório para gerenciar entidades ContaReceber
}