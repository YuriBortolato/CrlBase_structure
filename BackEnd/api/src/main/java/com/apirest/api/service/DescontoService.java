package com.apirest.api.service;

import com.apirest.api.entity.*;
import com.apirest.api.repository.VoucherRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class DescontoService {

    private final VoucherRepository voucherRepository;

    // Calcula o valor do desconto baseado no código do cupom e no valor bruto da venda.
    public BigDecimal calcularDescontoVoucher(String codigoCupom, BigDecimal valorVendaBruto) {
        if (codigoCupom == null || codigoCupom.isBlank()) return BigDecimal.ZERO;

        Voucher voucher = voucherRepository.findByCodigoAndAtivoTrue(codigoCupom.toUpperCase())
                .orElseThrow(() -> new RuntimeException("Cupom inválido ou inativo: " + codigoCupom));

        // Valida Data
        LocalDateTime agora = LocalDateTime.now();
        if (voucher.getValidadeInicio() != null && agora.isBefore(voucher.getValidadeInicio())) {
            throw new RuntimeException("Cupom ainda não está válido.");
        }
        if (voucher.getValidadeFim() != null && agora.isAfter(voucher.getValidadeFim())) {
            throw new RuntimeException("Cupom expirado.");
        }

        // Valida Quantidade
        if (voucher.getQuantidadeDisponivel() <= 0) {
            throw new RuntimeException("Cupom esgotado.");
        }

        // Calcula Valor
        BigDecimal desconto = BigDecimal.ZERO;
        if (voucher.getTipo() == Voucher.TipoDesconto.FIXO) {
            desconto = voucher.getValor();
        } else {
            // Percentual: (Total * Porcentagem) / 100
            desconto = valorVendaBruto.multiply(voucher.getValor()).divide(new BigDecimal("100"), 2, RoundingMode.HALF_EVEN);
        }

        // Trava de segurança: Desconto não pode ser maior que o total
        if (desconto.compareTo(valorVendaBruto) > 0) {
            desconto = valorVendaBruto;
        }

        return desconto;
    }

    @Transactional
    public void consumirVoucher(String codigoCupom) {
        if (codigoCupom == null) return;
        Voucher voucher = voucherRepository.findByCodigoAndAtivoTrue(codigoCupom.toUpperCase()).orElse(null);
        if (voucher != null) {
            voucher.setQuantidadeDisponivel(voucher.getQuantidadeDisponivel() - 1);
            voucherRepository.save(voucher);
        }
    }
}