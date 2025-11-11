package com.apirest.api.repository;

import com.apirest.api.entity.StatusVenda;
import com.apirest.api.entity.Venda;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VendaRepository extends JpaRepository<Venda, Long> {
    List<Venda> findByStatusVenda(StatusVenda status);
}

