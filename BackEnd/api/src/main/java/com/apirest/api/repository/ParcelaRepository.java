package com.apirest.api.repository;
import com.apirest.api.entity.Parcela;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ParcelaRepository extends JpaRepository<Parcela, Long> {
        // Repositório para gerenciar entidades Parcela
}