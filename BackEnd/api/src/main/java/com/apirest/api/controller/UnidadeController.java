package com.apirest.api.controller;

import com.apirest.api.entity.Unidade;
import com.apirest.api.service.UnidadeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/unidades")
@RequiredArgsConstructor
public class UnidadeController {

    private final UnidadeService service;

    @PostMapping
    public ResponseEntity<Unidade> criar(@RequestBody @Valid Unidade unidade) {
        Unidade novaUnidade = service.criarUnidade(unidade);
        return new ResponseEntity<>(novaUnidade, HttpStatus.CREATED);
    }
}