package com.apirest.api.service;

import com.apirest.api.dto.*;
import com.apirest.api.entity.*;
import com.apirest.api.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.apirest.api.entity.VendaPagamento;
import com.apirest.api.repository.VendaPagamentoRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class VendaService {
    private final VendaRepository vendaRepository;
    private final ClienteRepository clienteRepository;
    private final FuncionarioRepository funcionarioRepository;
    private final CaixaRepository caixaRepository;

    private final ProdutoVariacaoRepository produtoVariacaoRepository;
    private final EstoqueSaldoRepository estoqueSaldoRepository;

    private final DescontoService descontoService;
    private final VendaDescontoRepository vendaDescontoRepository;

    private final VendaPagamentoRepository vendaPagamentoRepository;

    private final ParcelaRepository parcelaRepository;

    private final PasswordEncoder passwordEncoder; // Para validar o PIN
    private final VendaEvidenciaRepository vendaEvidenciaRepository; // Para salvar a assinatura
    private final ContaReceberRepository contaReceberRepository; // Para checar a dívida

    private static final Set<String> PERMISSAO_GERENCIAR_VENDA = Set.of("DONO", "GERENTE", "LIDER_VENDA", "ADMIN");

    @Transactional
    public VendaResponseDTO registrarVenda(VendaDTO dto) {
        log.info("Iniciando registro de venda. Funcionario ID: {}, Cliente ID: {}", dto.getIdFuncionario(), dto.getIdCliente());

        // busca funcionário, validando ativo
        Funcionario funcionario = funcionarioRepository.findById(dto.getIdFuncionario())
                .orElseThrow(() -> new RuntimeException("Funcionário não encontrado"));
        if (!funcionario.isAtivo()) throw new RuntimeException("Funcionário inativo.");

        Unidade unidadeVenda = funcionario.getUnidade();

        // busca cliente, validando ativo
        Cliente cliente = clienteRepository.findById(dto.getIdCliente())
                .orElseThrow(() -> new RuntimeException("Cliente não encontrado"));
        if (!cliente.isAtivo()) throw new RuntimeException("Cliente inativo.");

        // busca caixa aberto do funcionário
        Caixa caixaAberto = caixaRepository.findByFuncionarioAndStatus(funcionario, StatusCaixa.ABERTO)
                .orElseThrow(() -> new RuntimeException("Não existe um caixa aberto. Abra o caixa antes de vender."));

        // cria venda
        Venda venda = Venda.builder()
                .funcionario(funcionario)
                .cliente(cliente)
                .caixa(caixaAberto)
                .metodoPagamento(dto.getMetodoPagamento())
                .observacoes(dto.getObservacoes())
                .dataVenda(LocalDateTime.now(ZoneId.of("America/Sao_Paulo")))
                .statusVenda(StatusVenda.REALIZADA)
                .build();

        BigDecimal valorBruto = BigDecimal.ZERO;
        List<VendaItem> itensVenda = new ArrayList<>();

        // processa cada item da venda
        for (VendaItemDTO itemDTO : dto.getItens()) {
            ProdutoVariacao variacao = produtoVariacaoRepository.findById(itemDTO.getIdProduto())
                    .orElseThrow(() -> new RuntimeException("Produto/Variação ID " + itemDTO.getIdProduto() + " não encontrado."));

            if (!variacao.isAtivo()) {
                throw new RuntimeException("Produto " + variacao.getNomeCompletoConcatenado() + " está inativo.");
            }

            EstoqueSaldo saldo = estoqueSaldoRepository.findByUnidadeIdAndProdutoVariacaoId(unidadeVenda.getIdUnidade(), variacao.getId())
                    .orElseThrow(() -> new RuntimeException("Produto sem cadastro de estoque nesta unidade."));

            if (saldo.getQuantidadeAtual() < itemDTO.getQuantidade()) {
                throw new RuntimeException("Estoque insuficiente para: " + variacao.getNomeCompletoConcatenado() +
                        ". Disponível: " + saldo.getQuantidadeAtual());
            }

            saldo.setQuantidadeAtual(saldo.getQuantidadeAtual() - itemDTO.getQuantidade());
            estoqueSaldoRepository.save(saldo);

            BigDecimal subtotal = variacao.getPrecoVenda().multiply(BigDecimal.valueOf(itemDTO.getQuantidade()));

            VendaItem vendaItem = VendaItem.builder()
                    .venda(venda)
                    .produtoVariacao(variacao)
                    .quantidade(itemDTO.getQuantidade())
                    .precoUnitario(variacao.getPrecoVenda())
                    .subtotal(subtotal)
                    .build();

            itensVenda.add(vendaItem);
            valorBruto = valorBruto.add(subtotal);
        }

        // associa itens à venda
        venda.setItens(itensVenda);

        BigDecimal totalDesconto = BigDecimal.ZERO;
        List<VendaDesconto> descontosParaSalvar = new ArrayList<>();

        // Aplica Cupom
        if (dto.getCodigoCupom() != null && !dto.getCodigoCupom().isBlank()) {
            BigDecimal valorDescCupom = descontoService.calcularDescontoVoucher(dto.getCodigoCupom(), valorBruto);
            totalDesconto = totalDesconto.add(valorDescCupom);

            descontosParaSalvar.add(VendaDesconto.builder()
                    .origem("VOUCHER")
                    .codigoReferencia(dto.getCodigoCupom())
                    .valorDescontoAplicado(valorDescCupom)
                    .build());

            descontoService.consumirVoucher(dto.getCodigoCupom());
        }

        // Aplica Manual (COM SEGURANÇA E ALÇADA)
        if (dto.getDescontoManual() != null && dto.getDescontoManual().compareTo(BigDecimal.ZERO) > 0) {

            // 1. Identifica o cargo (Já temos a variável 'funcionario' no início do método)
            String cargoAtual = funcionario.getCargo().name();

            // 2. Define quem tem poder ilimitado (Gerentes/Donos)
            boolean ehGerente = Set.of("DONO", "GERENTE", "ADMIN").contains(cargoAtual);

            // 3. Regra para Vendedores Comuns
            if (!ehGerente) {
                // Exemplo: Vendedor só pode dar até R$ 20,00 de desconto manual
                BigDecimal limiteVendedor = new BigDecimal("20.00");

                if (dto.getDescontoManual().compareTo(limiteVendedor) > 0) {
                    throw new RuntimeException("PERMISSÃO NEGADA: Seu cargo (" + cargoAtual +
                            ") só permite descontos manuais de até R$ " + limiteVendedor);
                }
            }

            // Se passou na validação, aplica o desconto
            totalDesconto = totalDesconto.add(dto.getDescontoManual());

            descontosParaSalvar.add(VendaDesconto.builder()
                    .venda(venda)
                    .origem("MANUAL")
                    .codigoReferencia("AUTORIZADO_" + cargoAtual)
                    .valorDescontoAplicado(dto.getDescontoManual())
                    .build());
        }

        // Calcula Líquido e Trava Valor Negativo
        BigDecimal valorLiquido = valorBruto.subtract(totalDesconto);
        if (valorLiquido.compareTo(BigDecimal.ZERO) < 0) {
            valorLiquido = BigDecimal.ZERO;
        }

        // Seta os valores finais na entidade
        venda.setValorBruto(valorBruto); // Certifique-se que criou esse campo em Venda.java
        venda.setValorTotal(valorLiquido); // O cliente paga o líquido

        Venda vendaSalva = vendaRepository.save(venda);


        for (VendaDesconto vd : descontosParaSalvar) {
            vd.setVenda(vendaSalva);
            vendaDescontoRepository.save(vd);
        }

        BigDecimal valorPago = dto.getValorPagoCliente();
        BigDecimal troco = BigDecimal.ZERO;

        // Se não informou valor (ex: cartão), assume pagamento exato
        if (valorPago == null || valorPago.compareTo(BigDecimal.ZERO) == 0) {
            valorPago = vendaSalva.getValorTotal();
        }

        // Validação básica
        if (valorPago.compareTo(vendaSalva.getValorTotal()) < 0) {
            throw new RuntimeException("Valor pago insuficiente. Total: " + vendaSalva.getValorTotal() + ", Pago: " + valorPago);
        }

        // Se for DINHEIRO, calcula troco. Se for PIX/CARTÃO, troco é zero.
        if (vendaSalva.getMetodoPagamento() == MetodoPagamento.DINHEIRO) {
            troco = valorPago.subtract(vendaSalva.getValorTotal());
        } else {
            valorPago = vendaSalva.getValorTotal(); // Ajusta para não registrar pagamento maior que a venda em cartão
        }

        // Salva o detalhe do pagamento
        VendaPagamento pagamento = VendaPagamento.builder()
                .venda(vendaSalva)
                .caixa(caixaAberto)
                .formaPagamento(vendaSalva.getMetodoPagamento())
                .valorPago(valorPago)
                .trocoGerado(troco)
                .valorLiquido(vendaSalva.getValorTotal())
                .build();

        vendaPagamentoRepository.save(pagamento);

        // Atualiza o troco na Venda (para consulta rápida)
        vendaSalva.setTrocoTotal(troco);
        vendaRepository.save(vendaSalva);

        if (vendaSalva.getMetodoPagamento() == MetodoPagamento.CREDIARIO) {

            // 1. REGRA: Só funcionário pode comprar no crediário
            // Verificamos se o Cliente tem vínculo com um Funcionário (campo funcionarioOrigem)
            if (cliente.getFuncionarioOrigem() == null) {
                throw new RuntimeException("BLOQUEIO: Venda no Crediário permitida apenas para Funcionários (Cliente não vinculado).");
            }
            Funcionario funcionarioComprador = cliente.getFuncionarioOrigem();

            // 2. REGRA: Validar PIN (Assinatura Digital)
            if (dto.getPin() == null || dto.getPin().isBlank()) {
                throw new RuntimeException("BLOQUEIO: O PIN é obrigatório para vendas no Crediário.");
            }
            // O PIN no banco já deve estar Hasheado (BCrypt). O match verifica se bate.
            if (!passwordEncoder.matches(dto.getPin(), funcionarioComprador.getPinHash())) {
                throw new RuntimeException("BLOQUEIO: PIN incorreto. Venda não autorizada.");
            }

            // 3. REGRA: Validar Limite de Crédito
            BigDecimal limiteDisponivel = cliente.getLimiteCredito();
            if (limiteDisponivel == null) limiteDisponivel = BigDecimal.ZERO;

            BigDecimal dividaAtual = contaReceberRepository.somarDividaAberta(cliente.getIdCliente());
            if (dividaAtual == null) dividaAtual = BigDecimal.ZERO;

            BigDecimal novaDivida = dividaAtual.add(vendaSalva.getValorTotal());

            if (novaDivida.compareTo(limiteDisponivel) > 0) {
                throw new RuntimeException(String.format("BLOQUEIO: Limite Excedido. Limite: R$ %s | Dívida Atual: R$ %s | Tentativa: R$ %s",
                        limiteDisponivel, dividaAtual, vendaSalva.getValorTotal()));
            }

            // 4. REGRA: Salvar Evidência (Assinatura)
            if (dto.getAssinaturaBase64() != null && !dto.getAssinaturaBase64().isBlank()) {
                VendaEvidencia evidencia = VendaEvidencia.builder()
                        .venda(vendaSalva)
                        .assinaturaBase64(dto.getAssinaturaBase64())
                        .dataRegistro(LocalDateTime.now())
                        .build();
                vendaEvidenciaRepository.save(evidencia);
            } else {
                throw new RuntimeException("BLOQUEIO: Assinatura obrigatória para Crediário.");
            }

            // 5. REGRA: Não gera comissão (Venda interna)
            // Se tiver campo de comissão, zerar aqui

            // --- GERAÇÃO DAS PARCELAS ---
            int qtdParcelas = (dto.getNumeroParcelas() != null) ? dto.getNumeroParcelas() : 1;
            BigDecimal valorTotalCrediario = vendaSalva.getValorTotal();

            ContaReceber conta = ContaReceber.builder()
                    .venda(vendaSalva)
                    .cliente(cliente)
                    .valorTotal(valorTotalCrediario)
                    .quantidadeParcelas(qtdParcelas)
                    .status(ContaReceber.StatusConta.ABERTA)
                    .dataCriacao(LocalDateTime.now(ZoneId.of("America/Sao_Paulo")))
                    .build();
            conta = contaReceberRepository.save(conta);

            BigDecimal valorBaseParcela = valorTotalCrediario.divide(BigDecimal.valueOf(qtdParcelas), 2, java.math.RoundingMode.DOWN);
            BigDecimal totalParcelado = valorBaseParcela.multiply(BigDecimal.valueOf(qtdParcelas));
            BigDecimal diferencaCentavos = valorTotalCrediario.subtract(totalParcelado);
            java.time.LocalDate dataBaseVencimento = java.time.LocalDate.now();

            for (int i = 1; i <= qtdParcelas; i++) {
                BigDecimal valorParcela = valorBaseParcela;
                if (i == 1) valorParcela = valorParcela.add(diferencaCentavos);

                Parcela parcela = Parcela.builder()
                        .contaReceber(conta)
                        .numeroParcela(i)
                        .valorOriginal(valorParcela)
                        .valorPago(BigDecimal.ZERO)
                        .status(Parcela.StatusParcela.PENDENTE)
                        .dataVencimento(dataBaseVencimento.plusDays(30L * i))
                        .build();
                parcelaRepository.save(parcela);
            }
            log.info("Crediário Funcionário autorizado: Conta ID {}", conta.getId());
        }

        return toResponseDTO(vendaSalva);
    }

    @Transactional
    public VendaResponseDTO cancelarVenda(Long idVenda, Long idFuncionario) {
        validarPermissao(idFuncionario, PERMISSAO_GERENCIAR_VENDA, "cancelar venda");

        Venda venda = findVendaById(idVenda);
        if (venda.getStatusVenda() == StatusVenda.CANCELADA) throw new RuntimeException("Venda já cancelada.");
        if (venda.getCaixa().getStatus() == StatusCaixa.FECHADO) throw new RuntimeException("Caixa já fechado.");

        // Unidade onde a venda ocorreu
        Unidade unidadeVenda = venda.getFuncionario().getUnidade();

        // Estorno
        for (VendaItem item : venda.getItens()) {
            ProdutoVariacao variacao = item.getProdutoVariacao();

            EstoqueSaldo saldo = estoqueSaldoRepository.findByUnidadeIdAndProdutoVariacaoId(unidadeVenda.getIdUnidade(), variacao.getId())
                    .orElseThrow(() -> new RuntimeException("Erro crítico: Estoque não encontrado para estorno."));

            saldo.setQuantidadeAtual(saldo.getQuantidadeAtual() + item.getQuantidade());
            estoqueSaldoRepository.save(saldo);
        }

        venda.setStatusVenda(StatusVenda.CANCELADA);
        return toResponseDTO(vendaRepository.save(venda));
    }

    @Transactional
    public VendaResponseDTO reativarVenda(Long idVenda, Long idFuncionario) {
        validarPermissao(idFuncionario, PERMISSAO_GERENCIAR_VENDA, "reativar venda");

        Venda venda = findVendaById(idVenda);
        if (venda.getStatusVenda() == StatusVenda.REALIZADA) throw new RuntimeException("Venda já realizada.");
        if (venda.getCaixa().getStatus() == StatusCaixa.FECHADO) throw new RuntimeException("Caixa já fechado.");

        Unidade unidadeVenda = venda.getFuncionario().getUnidade();

        for (VendaItem item : venda.getItens()) {
            ProdutoVariacao variacao = item.getProdutoVariacao();
            if (variacao == null || !variacao.isAtivo()) {
                throw new RuntimeException("Produto não existe ou está inativo. Impossível reativar.");
            }

            EstoqueSaldo saldo = estoqueSaldoRepository.findByUnidadeIdAndProdutoVariacaoId(unidadeVenda.getIdUnidade(), variacao.getId())
                    .orElseThrow(() -> new RuntimeException("Estoque não encontrado para reativar."));

            if (saldo.getQuantidadeAtual() < item.getQuantidade()) {
                throw new RuntimeException("Estoque insuficiente para reativar: " + variacao.getNomeCompletoConcatenado());
            }

            saldo.setQuantidadeAtual(saldo.getQuantidadeAtual() - item.getQuantidade());
            estoqueSaldoRepository.save(saldo);
        }

        venda.setStatusVenda(StatusVenda.REALIZADA);
        return toResponseDTO(vendaRepository.save(venda));
    }

    // LISTAR E AUXILIARES
    public List<VendaResponseDTO> listarVendas(StatusVenda status) {
        List<Venda> vendas = (status != null) ? vendaRepository.findByStatusVenda(status) : vendaRepository.findAll();
        return vendas.stream().map(this::toResponseDTO).collect(Collectors.toList());
    }

    public VendaResponseDTO buscarPorId(Long id) {
        return toResponseDTO(findVendaById(id));
    }

    private Venda findVendaById(Long id) {
        return vendaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Venda não encontrada"));
    }

    private void validarPermissao(Long idFuncionario, Set<String> cargos, String acao) {
        Funcionario f = funcionarioRepository.findById(idFuncionario).orElseThrow();
        if (!cargos.contains(f.getCargo().name())) throw new RuntimeException("Sem permissão para " + acao);
    }

    private VendaResponseDTO toResponseDTO(Venda v) {
        List<VendaItemResponseDTO> itensDTO = v.getItens().stream().map(item -> new VendaItemResponseDTO(
                item.getProdutoVariacao().getId(),
                item.getProdutoVariacao().getNomeCompletoConcatenado(),
                item.getQuantidade(),
                item.getPrecoUnitario(),
                item.getSubtotal()
        )).collect(Collectors.toList());

        return new VendaResponseDTO(
                v.getIdVenda(),
                v.getFuncionario().getIdFuncionario(),
                v.getFuncionario().getNomeCompleto(),
                v.getCliente().getIdCliente(),
                v.getCliente().getNomeCompleto(),
                itensDTO,
                v.getValorTotal(),
                v.getDataVenda(),
                v.getMetodoPagamento(),
                v.getStatusVenda(),
                v.getObservacoes()
        );
    }
}