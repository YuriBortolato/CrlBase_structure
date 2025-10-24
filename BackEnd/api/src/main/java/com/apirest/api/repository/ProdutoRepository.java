package com.apirest.api.repository;


import com.apirest.api.entity.Produto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProdutoRepository extends JpaRepository<Produto, Long> {
    boolean existsByNomeAndAtivoTrue(String nome);
    List<Produto> findAllByAtivoTrue();
    Optional<Produto> findByIdProdutoAndAtivoTrue(Long id);
}