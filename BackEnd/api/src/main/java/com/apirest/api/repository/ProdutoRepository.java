package com.apirest.api.repository;

import com.apirest.api.entity.Produto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProdutoRepository extends JpaRepository<Produto, Long> { // Produto é a entidade e Long é o tipo do ID
    boolean existsByNome(String nome); // Verifica se um produto com o nome existe
}
