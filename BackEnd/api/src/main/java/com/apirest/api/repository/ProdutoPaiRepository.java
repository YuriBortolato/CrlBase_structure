package com.apirest.api.repository;

import com.apirest.api.entity.ProdutoPai;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProdutoPaiRepository extends JpaRepository<ProdutoPai, Long> {
}