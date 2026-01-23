package com.apirest.api.repository;

import com.apirest.api.entity.Unidade;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UnidadeRepository extends JpaRepository<Unidade, Long> {
    long count(); // Metodo para contar o número de unidades cadastradas
    boolean existsByDocumentoNumero(String documento);
}