package com.apirest.api.service;

import com.apirest.api.dto.ContaReceberResponseDTO;
import com.apirest.api.dto.ParcelaDTO;
import com.apirest.api.entity.Cliente;
import com.apirest.api.entity.ContaReceber;
import com.apirest.api.repository.ClienteRepository;
import com.apirest.api.repository.ContaReceberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ContaReceberService {

    private final ContaReceberRepository contaReceberRepository;
    private final ClienteRepository clienteRepository;

    // Buscar todas as contas de um cliente (Histórico financeiro)
    @Transactional(readOnly = true)
    public List<ContaReceberResponseDTO> buscarPorCliente(Long idCliente) {
        Cliente cliente = clienteRepository.findById(idCliente)
                .orElseThrow(() -> new RuntimeException("Cliente não encontrado"));

        // Buscar todas as contas a receber associadas ao cliente
        List<ContaReceber> contas = contaReceberRepository.findByCliente(cliente);

        return contas.stream()
                .map(this::toResponseDTO)
                .collect(Collectors.toList());
    }

    // Buscar detalhes de uma conta específica
    @Transactional(readOnly = true)
    public ContaReceberResponseDTO buscarPorId(Long idConta) {
        ContaReceber conta = contaReceberRepository.findById(idConta)
                .orElseThrow(() -> new RuntimeException("Conta a receber não encontrada"));

        return toResponseDTO(conta);
    }

    // Conversor Entity -> DTO
    private ContaReceberResponseDTO toResponseDTO(ContaReceber c) {
        List<ParcelaDTO> parcelasDTO = c.getParcelas().stream()
                .map(p -> ParcelaDTO.builder()
                        .numero(p.getNumeroParcela())
                        .valorOriginal(p.getValorOriginal())
                        .valorPago(p.getValorPago())
                        .dataVencimento(p.getDataVencimento())
                        .status(p.getStatus().name())
                        .build())
                .collect(Collectors.toList());

        return ContaReceberResponseDTO.builder()
                .idConta(c.getId())
                .idVendaOrigem(c.getVenda().getIdVenda())
                .nomeCliente(c.getCliente().getNomeCompleto())
                .cpfCliente(c.getCliente().getCpf())
                .valorTotal(c.getValorTotal())
                .quantidadeParcelas(c.getQuantidadeParcelas())
                .statusConta(c.getStatus().name())
                .dataCriacao(c.getDataCriacao())
                .parcelas(parcelasDTO)
                .build();
    }
}