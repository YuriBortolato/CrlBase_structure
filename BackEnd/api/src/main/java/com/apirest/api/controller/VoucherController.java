package com.apirest.api.controller;

import com.apirest.api.dto.VoucherDTO;
import com.apirest.api.entity.Voucher;
import com.apirest.api.service.VoucherService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/vouchers")
@RequiredArgsConstructor
public class VoucherController {

    private final VoucherService voucherService;

    @PostMapping
    public ResponseEntity<Voucher> criar(@RequestBody @Valid VoucherDTO dto) {
        Voucher novoVoucher = voucherService.criarVoucher(dto);
        return new ResponseEntity<>(novoVoucher, HttpStatus.CREATED);
    }
}