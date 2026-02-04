package com.apirest.api.service;

import com.apirest.api.dto.VoucherDTO;
import com.apirest.api.entity.Funcionario;
import com.apirest.api.entity.Voucher;
import com.apirest.api.repository.FuncionarioRepository;
import com.apirest.api.repository.VoucherRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class VoucherService {

    private final VoucherRepository voucherRepository;
    private final FuncionarioRepository funcionarioRepository;

    private static final Set<String> PERMISSAO_CRIAR_VOUCHER = Set.of("DONO", "GERENTE", "ADMIN");

    @Transactional
    public Voucher criarVoucher(VoucherDTO dto) {
        Funcionario criador = funcionarioRepository.findById(dto.getIdFuncionarioCriador())
                .orElseThrow(() -> new RuntimeException("Funcionário criador não encontrado."));

        if (!PERMISSAO_CRIAR_VOUCHER.contains(criador.getCargo().name())) {
            throw new RuntimeException("Acesso Negado: Apenas Gerentes/Donos podem criar campanhas promocionais.");
        }

        Optional<Voucher> existente = voucherRepository.findByCodigoAndAtivoTrue(dto.getCodigo().toUpperCase());
        if (existente.isPresent()) {
            throw new RuntimeException("Já existe um voucher ativo com o código " + dto.getCodigo());
        }

        Voucher voucher = Voucher.builder()
                .codigo(dto.getCodigo().toUpperCase())
                .tipo(Voucher.TipoDesconto.valueOf(dto.getTipo()))
                .valor(dto.getValor())
                .quantidadeDisponivel(dto.getQuantidadeDisponivel())
                .validadeInicio(LocalDateTime.now())
                .validadeFim(dto.getValidadeFim())
                .ativo(true)
                .acumulativo(dto.isAcumulativo())
                .build();

        return voucherRepository.save(voucher);
    }
}