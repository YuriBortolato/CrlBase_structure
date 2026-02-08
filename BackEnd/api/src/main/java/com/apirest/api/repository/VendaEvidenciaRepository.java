package com.apirest.api.repository;

import com.apirest.api.entity.VendaEvidencia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface VendaEvidenciaRepository extends JpaRepository<VendaEvidencia, Long> {
}