package com.apirest.api.repository;

import com.apirest.api.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProdutoRepository extends JpaRepository<Produto, Long> { // Produto é a entidade e Long é o tipo do ID
    boolean existsByName(String nome); // Exemplo de método personalizado para verificar se um produto existe pelo nome
}
