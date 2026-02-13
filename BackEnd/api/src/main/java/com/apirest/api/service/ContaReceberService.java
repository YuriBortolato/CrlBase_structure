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
        System.out.println("--- INICIANDO PAGAMENTO EM CASCATA ---");

        // Buscar a parcela inicial e a conta mãe
        Parcela parcelaInicial = parcelaRepository.findById(dto.getIdParcela())
                .orElseThrow(() -> new RuntimeException("Parcela não encontrada"));

        ContaReceber conta = parcelaInicial.getContaReceber();

        // Validar Caixa Aberto
        Funcionario recebedor = funcionarioRepository.findById(dto.getIdFuncionarioRecebedor())
                .orElseThrow(() -> new RuntimeException("Funcionário recebedor não encontrado"));

        Caixa caixa = caixaRepository.findByFuncionarioAndStatus(recebedor, StatusCaixa.ABERTO)
                .orElseThrow(() -> new RuntimeException("Operação negada: O funcionário " + recebedor.getNomeCompleto() + " não possui um caixa aberto."));

        // Buscar TODAS as parcelas dessa conta, ordenadas pelo número (1, 2, 3...)
        List<Parcela> todasParcelas = conta.getParcelas().stream()
                .sorted((p1, p2) -> p1.getNumeroParcela().compareTo(p2.getNumeroParcela()))
                .collect(Collectors.toList());

        // Calcular Dívida Total Restante da Conta
        BigDecimal dividaTotal = BigDecimal.ZERO;
        for (Parcela p : todasParcelas) {
            BigDecimal saldoParcela = p.getValorOriginal().subtract(p.getValorPago() != null ? p.getValorPago() : BigDecimal.ZERO);
            if (saldoParcela.compareTo(BigDecimal.ZERO) > 0) {
                dividaTotal = dividaTotal.add(saldoParcela);
            }
        }

        // Validação: Não aceitar valor maior que a dívida total (criar crédito somente com tabela pra isso)
        if (dto.getValorPago().compareTo(dividaTotal) > 0) {
            throw new RuntimeException("Valor excede a dívida total da venda! Valor máximo aceito: R$ " + dividaTotal);
        }

        // LÓGICA DA CASCATA (WATERFALL)
        BigDecimal valorDisponivel = dto.getValorPago();
        StringBuilder historicoPagamento = new StringBuilder();

        for (Parcela p : todasParcelas) {
            // Se já não temos mais dinheiro para alocar, paramos o processo
            if (valorDisponivel.compareTo(BigDecimal.ZERO) <= 0) break;

            // Se a parcela já está paga, pula para a próxima
            if (p.getStatus() == Parcela.StatusParcela.PAGA) continue;

            // Calcula quanto falta pagar nessa parcela
            BigDecimal valorJaPagoNesta = p.getValorPago() != null ? p.getValorPago() : BigDecimal.ZERO;
            BigDecimal saldoDevedorNesta = p.getValorOriginal().subtract(valorJaPagoNesta);

            BigDecimal valorParaAbater;

            if (valorDisponivel.compareTo(saldoDevedorNesta) >= 0) {
                // O dinheiro é suficiente para quitar esta parcela
                valorParaAbater = saldoDevedorNesta;
                p.setStatus(Parcela.StatusParcela.PAGA);
                historicoPagamento.append("P").append(p.getNumeroParcela()).append("(Quitada) ");
            } else {
                // O dinheiro não é suficiente para quitar, mas é suficiente para abater parte dela
                valorParaAbater = valorDisponivel;
                // Status continua PENDENTE, mas com valor pago parcial
                historicoPagamento.append("P").append(p.getNumeroParcela()).append("(Parcial) ");
            }

            // Atualiza a parcela
            p.setValorPago(valorJaPagoNesta.add(valorParaAbater));
            p.setDataPagamento(LocalDateTime.now());
            parcelaRepository.save(p); // Salva a parcela atualizada

            // Abate o valor disponível
            valorDisponivel = valorDisponivel.subtract(valorParaAbater);
        }

        // Força sincronização
        parcelaRepository.flush();

        // Lançar movimentação no caixa com o valor total pago e histórico detalhado
        String motivoLancamento = String.format("Recebimento Crediário | Cliente: %s | %s | Total: R$ %s",
                conta.getCliente().getNomeCompleto(),
                historicoPagamento.toString(),
                dto.getValorPago());

        // Garantir que o motivo do lançamento não ultrapasse 255 caracteres (limite do banco)
        if (motivoLancamento.length() > 255) motivoLancamento = motivoLancamento.substring(0, 255);

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
        boolean todasPagas = todasParcelas.stream()
                .allMatch(p -> p.getStatus() == Parcela.StatusParcela.PAGA);

        if (todasPagas) {
            System.out.println("Conta TOTALMENTE QUITADA! ID: " + conta.getId());
            conta.setStatus(ContaReceber.StatusConta.QUITADA);
            contaReceberRepository.save(conta);
        } else {
            // Se ainda restar alguma parcela pendente, garantir que o status da conta seja ABERTA (caso estivesse como QUITADA por algum erro anterior)
            conta.setStatus(ContaReceber.StatusConta.ABERTA);
            contaReceberRepository.save(conta);
        }

        System.out.println("--- PAGAMENTO EM CASCATA FINALIZADO ---");
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