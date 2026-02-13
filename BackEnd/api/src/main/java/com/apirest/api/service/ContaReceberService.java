package com.apirest.api.service;

import com.apirest.api.dto.ContaReceberResponseDTO;
import com.apirest.api.dto.PagamentoParcelaDTO;
import com.apirest.api.dto.ParcelaDTO;
import com.apirest.api.entity.*;
import com.apirest.api.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ContaReceberService {

    private final ContaReceberRepository contaReceberRepository;
    private final ClienteRepository clienteRepository;
    private final ParcelaRepository parcelaRepository;
    private final CaixaRepository caixaRepository;
    private final CaixaMovimentacaoRepository caixaMovimentacaoRepository;
    private final FuncionarioRepository funcionarioRepository;

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

    @Transactional
    public void pagarParcela(PagamentoParcelaDTO dto) {
        System.out.println("--- INICIANDO PROCESSO DE PAGAMENTO ---");

        // Validar Parcela
        Parcela parcela = parcelaRepository.findById(dto.getIdParcela())
                .orElseThrow(() -> new RuntimeException("Parcela não encontrada com ID: " + dto.getIdParcela()));

        if (parcela.getStatus() == Parcela.StatusParcela.PAGA) {
            throw new RuntimeException("Esta parcela já consta como PAGA no sistema.");
        }

        // Validar Caixa Aberto (O dinheiro tem que entrar em algum lugar)
        Funcionario recebedor = funcionarioRepository.findById(dto.getIdFuncionarioRecebedor())
                .orElseThrow(() -> new RuntimeException("Funcionário recebedor não encontrado"));

        // Busca o caixa ABERTO deste funcionário
        Caixa caixa = caixaRepository.findByFuncionarioAndStatus(recebedor, StatusCaixa.ABERTO)
                .orElseThrow(() -> new RuntimeException("Operação negada: O funcionário " + recebedor.getNomeCompleto() + " não possui um caixa aberto."));


        //LÓGICA DE PAGAMENTO PARCIAL E BAIXA
        BigDecimal valorJaPago = parcela.getValorPago() != null ? parcela.getValorPago() : BigDecimal.ZERO;
        BigDecimal novoTotalPago = valorJaPago.add(dto.getValorPago());

        // Atualiza o valor pago acumulado
        parcela.setValorPago(novoTotalPago);
        parcela.setDataPagamento(LocalDateTime.now());

        // Verifica se quitou o valor original
        if (novoTotalPago.compareTo(parcela.getValorOriginal()) >= 0) {
            System.out.println("Pagamento total atingido. Baixando parcela ID: " + parcela.getId());
            parcela.setStatus(Parcela.StatusParcela.PAGA);
        } else {
            System.out.println("Pagamento PARCIAL. Resta pagar: " + parcela.getValorOriginal().subtract(novoTotalPago));
            // Mantém status PENDENTE
        }

        // Persiste a alteração
        parcelaRepository.saveAndFlush(parcela);

        // Atualizar a Parcela
        // Lançar Entrada no Caixa (Movimentação)
        String motivoLancamento = String.format("Recebimento Crediário | Parcela %d de %d | Cliente: %s | Pgto: %s",
                parcela.getNumeroParcela(),
                parcela.getContaReceber().getQuantidadeParcelas(),
                parcela.getContaReceber().getCliente().getNomeCompleto(),
                dto.getMetodoPagamento());

        CaixaMovimentacao movimentacao = CaixaMovimentacao.builder()
                .caixa(caixa)
                .tipo(CaixaMovimentacao.TipoMovimentacao.ENTRADA)
                .motivo(motivoLancamento)
                .valor(dto.getValorPago())
                .dataHora(LocalDateTime.now())
                .usuarioAutorizador(recebedor)
                .build();

        caixaMovimentacaoRepository.save(movimentacao);


        // Verificar se a Conta inteira foi quitada
        ContaReceber conta = parcela.getContaReceber();

        // Verifica se todas as parcelas desta conta estão com status PAGA
        boolean todasPagas = conta.getParcelas().stream()
                .allMatch(p -> p.getStatus() == Parcela.StatusParcela.PAGA);

        if (todasPagas) {
            System.out.println("Todas as parcelas pagas. Quitando a conta ID: " + conta.getId());
            conta.setStatus(ContaReceber.StatusConta.QUITADA);
            contaReceberRepository.save(conta);
        }

        System.out.println("--- PAGAMENTO FINALIZADO COM SUCESSO ---");
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
                        .id(p.getId())
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