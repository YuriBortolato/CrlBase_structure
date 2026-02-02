package com.apirest.api.service;

import com.apirest.api.dto.*;
import com.apirest.api.entity.*;
import com.apirest.api.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

        // Aplica Manual
        if (dto.getDescontoManual() != null && dto.getDescontoManual().compareTo(BigDecimal.ZERO) > 0) {
            totalDesconto = totalDesconto.add(dto.getDescontoManual());

            descontosParaSalvar.add(VendaDesconto.builder()
                    .origem("MANUAL")
                    .codigoReferencia("VENDEDOR")
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