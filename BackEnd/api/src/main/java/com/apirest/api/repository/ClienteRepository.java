package com.apirest.api.repository;

import com.apirest.api.entity.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ClienteRepository extends JpaRepository<Cliente, Long> {
    boolean existsByEmail(String email);
    boolean existsByCpf(String cpf);
    boolean existsByLogin(String login);
    List<Cliente> findAllByAtivoTrue();

    Optional<Cliente> findByCpf(String cpf);
}