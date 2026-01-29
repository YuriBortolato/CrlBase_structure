package com.apirest.api.repository;

import com.apirest.api.entity.PerfilAcesso;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PerfilAcessoRepository extends JpaRepository<PerfilAcesso, Long> {
    Optional<PerfilAcesso> findByNome(String nome);
}