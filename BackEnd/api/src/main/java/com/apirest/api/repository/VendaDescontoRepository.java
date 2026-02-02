package com.apirest.api.repository;

import com.apirest.api.entity.VendaDesconto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface VendaDescontoRepository extends JpaRepository<VendaDesconto, Long> {

}