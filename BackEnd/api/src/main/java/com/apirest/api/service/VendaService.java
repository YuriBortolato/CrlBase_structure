package com.apirest.api.service;

import com.apirest.api.dto.VendaDTO;
import com.apirest.api.dto.VendaItemDTO;
import com.apirest.api.dto.VendaItemResponseDTO;
import com.apirest.api.dto.VendaResponseDTO;
import com.apirest.api.entity.*;
import com.apirest.api.repository.CaixaRepository;
import com.apirest.api.repository.ClienteRepository;
import com.apirest.api.repository.FuncionarioRepository;
import com.apirest.api.repository.ProdutoRepository;
import com.apirest.api.repository.VendaRepository;
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
    private final ProdutoRepository produtoRepository;

    // Definição das permissões necessárias para gerenciar vendas
    private final CaixaRepository caixaRepository;

    private static final Set<String> PERMISSAO_GERENCIAR_VENDA = Set.of("DONO", "GERENTE", "LIDER_VENDA", "ADMIN");

    @Transactional
    public VendaResponseDTO registrarVenda(VendaDTO dto) {
        log.info("Iniciando registro de venda. Funcionario ID: {}, Cliente ID: {}", dto.getIdFuncionario(), dto.getIdCliente());

        // busca produto, validando funcionário ativo
        Funcionario funcionario = funcionarioRepository.findById(dto.getIdFuncionario())
                .orElseThrow(() -> new RuntimeException("Funcionário com ID " + dto.getIdFuncionario() + " não encontrado"));
        if (!funcionario.isAtivo()) {
            log.warn("Tentativa de venda por funcionário inativo: {}", funcionario.getNomeCompleto());
            throw new RuntimeException("Funcionário " + funcionario.getNomeCompleto() + " está inativo.");
        }

        // busca produto, validando cliente ativo
        Cliente cliente = clienteRepository.findById(dto.getIdCliente())
                .orElseThrow(() -> new RuntimeException("Cliente com ID " + dto.getIdCliente() + " não encontrado"));
        if (!cliente.isAtivo()) {
            log.warn("Tentativa de venda para cliente inativo: {}", cliente.getNomeCompleto());
            throw new RuntimeException("Cliente " + cliente.getNomeCompleto() + " está inativo.");
        }

        Caixa caixaAberto = caixaRepository.findByFuncionarioAndStatus(funcionario, StatusCaixa.ABERTO)
                .orElseThrow(() -> new RuntimeException("Não existe um caixa aberto para este funcionário. Abra o caixa antes de vender."));

        // Cria a Venda e Vincula tudo
        Venda venda = new Venda();
        venda.setFuncionario(funcionario);
        venda.setCliente(cliente);
        venda.setCaixa(caixaAberto);
        venda.setMetodoPagamento(dto.getMetodoPagamento());
        venda.setObservacoes(dto.getObservacoes());

        // Configuração de Fuso Horário Correto
        venda.setDataVenda(LocalDateTime.now(ZoneId.of("America/Sao_Paulo")));
        venda.setStatusVenda(StatusVenda.REALIZADA);

        BigDecimal valorTotalVenda = BigDecimal.ZERO;
        List<VendaItem> itensVenda = new ArrayList<>();

        // Processa cada item da venda
        for (VendaItemDTO itemDTO : dto.getItens()) {
            Produto produto = produtoRepository.findByIdProdutoAndAtivoTrue(itemDTO.getIdProduto())
                    .orElseThrow(() -> new RuntimeException("Produto com ID " + itemDTO.getIdProduto() + " não encontrado ou inativo."));

            // Verifica estoque disponível
            if (produto.getQuantidadeEmEstoque() < itemDTO.getQuantidade()) {
                log.error("Estoque insuficiente. Produto: {}, Estoque: {}, Solicitado: {}",
                        produto.getNome(), produto.getQuantidadeEmEstoque(), itemDTO.getQuantidade());
                throw new RuntimeException("Estoque insuficiente para o produto: " + produto.getNome());
            }

            log.info("Debitando estoque. Produto: {}, Qtd Anterior: {}, Qtd Debitada: {}",
                    produto.getNome(), produto.getQuantidadeEmEstoque(), itemDTO.getQuantidade());

            // Atualiza o estoque do produto
            produto.setQuantidadeEmEstoque(produto.getQuantidadeEmEstoque() - itemDTO.getQuantidade());
            produtoRepository.save(produto);

            // Calcula subtotal
            BigDecimal precoUnitario = produto.getValorVenda();
            BigDecimal subtotal = precoUnitario.multiply(BigDecimal.valueOf(itemDTO.getQuantidade()));

            // Cria o item da venda
            VendaItem vendaItem = new VendaItem();
            vendaItem.setVenda(venda);
            vendaItem.setProduto(produto);
            vendaItem.setQuantidade(itemDTO.getQuantidade());
            vendaItem.setPrecoUnitario(precoUnitario);
            vendaItem.setSubtotal(subtotal);

            itensVenda.add(vendaItem);
            valorTotalVenda = valorTotalVenda.add(subtotal);
        }

        // Finaliza a venda
        venda.setItens(itensVenda);
        venda.setValorTotal(valorTotalVenda);
        Venda vendaSalva = vendaRepository.save(venda);

        // Retorna o DTO de resposta
        log.info("Venda realizada com SUCESSO! ID: {}, Valor Total: {}", vendaSalva.getIdVenda(), vendaSalva.getValorTotal());
        return toResponseDTO(vendaSalva);
    }

    // Cancelar Venda
    @Transactional
    public VendaResponseDTO cancelarVenda(Long idVenda, Long idFuncionario) {
        log.info("Solicitação de cancelamento da Venda ID: {} pelo Funcionario ID: {}", idVenda, idFuncionario);

        validarPermissao(idFuncionario, PERMISSAO_GERENCIAR_VENDA, "cancelar venda");

        Venda venda = findVendaById(idVenda);
        if (venda.getStatusVenda() == StatusVenda.CANCELADA) {
            throw new RuntimeException("Venda " + idVenda + " já está cancelada.");
        }
        // --- Bloqueia cancelamento se o caixa estiver FECHADO ---
        if (venda.getCaixa().getStatus() == StatusCaixa.FECHADO) {
            log.warn("Bloqueio: Tentativa de cancelar venda ID {} associada a um caixa já FECHADO (ID {}).", idVenda, venda.getCaixa().getIdCaixa());
            throw new RuntimeException("Não é possível cancelar esta venda pois o caixa já foi fechado e conferido.");
        }

        for (VendaItem item : venda.getItens()) {
            Produto produto = item.getProduto();
            if (produto != null && produtoRepository.existsById(produto.getIdProduto())) {
                produto.setQuantidadeEmEstoque(produto.getQuantidadeEmEstoque() + item.getQuantidade());
                produtoRepository.save(produto);
            }
        }

        venda.setStatusVenda(StatusVenda.CANCELADA);
        Venda vendaSalva = vendaRepository.save(venda);
        log.info("Venda ID {} cancelada com sucesso.", idVenda);
        return toResponseDTO(vendaSalva);
    }

    @Transactional
    public VendaResponseDTO reativarVenda(Long idVenda, Long idFuncionario) {
        log.info("Solicitação de reativação da Venda ID: {} pelo Funcionario ID: {}", idVenda, idFuncionario);

        validarPermissao(idFuncionario, PERMISSAO_GERENCIAR_VENDA, "reativar venda");

        // --- Verifica se a venda pode ser reativada ---
        Venda venda = findVendaById(idVenda);
        if (venda.getStatusVenda() == StatusVenda.REALIZADA) {
            throw new RuntimeException("Venda " + idVenda + " já está realizada.");
        }

        // --- Bloqueia reativação se o caixa estiver FECHADO ---
        if (venda.getCaixa().getStatus() == StatusCaixa.FECHADO) {
            throw new RuntimeException("Não é possível reativar esta venda pois o caixa já foi fechado.");
        }

        // --- Verifica estoque disponível para reativação ---
        for (VendaItem item : venda.getItens()) {
            Produto produto = item.getProduto();
            if (produto == null) {
                throw new RuntimeException("Não é possível reativar: O produto associado não existe mais.");
            }

            Produto produtoAtivo = produtoRepository.findByIdProdutoAndAtivoTrue(produto.getIdProduto())
                    .orElseThrow(() -> new RuntimeException("Não é possível reativar: O produto " + produto.getNome() + " está inativo."));

            // Verifica estoque disponível
            if (produtoAtivo.getQuantidadeEmEstoque() < item.getQuantidade()) {
                throw new RuntimeException("Estoque insuficiente para reativar venda: " + produtoAtivo.getNome());
            }

            produtoAtivo.setQuantidadeEmEstoque(produtoAtivo.getQuantidadeEmEstoque() - item.getQuantidade());
            produtoRepository.save(produtoAtivo);
        }

        venda.setStatusVenda(StatusVenda.REALIZADA);
        Venda vendaSalva = vendaRepository.save(venda);
        log.info("Venda ID {} reativada com sucesso.", idVenda);
        return toResponseDTO(vendaSalva);
    }

    public List<VendaResponseDTO> listarVendas(StatusVenda status) {
        List<Venda> vendas;
        if (status != null) {
            vendas = vendaRepository.findByStatusVenda(status);
        } else {
            vendas = vendaRepository.findAll();
        }
        return vendas.stream().map(this::toResponseDTO).collect(Collectors.toList());
    }

    public VendaResponseDTO buscarPorId(Long id) {
        Venda venda = findVendaById(id);
        return toResponseDTO(venda);
    }

    private Venda findVendaById(Long id) {
        return vendaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Venda com ID " + id + " não encontrada"));
    }

    private void validarPermissao(Long idFuncionario, Set<String> cargosPermitidos, String acao) {
        Funcionario funcionario = funcionarioRepository.findById(idFuncionario)
                .orElseThrow(() -> new RuntimeException("Funcionário não encontrado"));

        if (funcionario.getCargo() == null || !cargosPermitidos.contains(funcionario.getCargo().name())) {
            throw new RuntimeException("Permissão negada: cargo não autorizado a " + acao);
        }
    }

    private VendaResponseDTO toResponseDTO(Venda venda) {
        List<VendaItemResponseDTO> itensDTO = venda.getItens().stream()
                .map(item -> new VendaItemResponseDTO(
                        item.getProduto().getIdProduto(),
                        item.getProduto().getNome(),
                        item.getQuantidade(),
                        item.getPrecoUnitario(),
                        item.getSubtotal()
                )).collect(Collectors.toList());

        return new VendaResponseDTO(
                venda.getIdVenda(),
                venda.getFuncionario().getIdFuncionario(),
                venda.getFuncionario().getNomeCompleto(),
                venda.getCliente().getIdCliente(),
                venda.getCliente().getNomeCompleto(),
                itensDTO,
                venda.getValorTotal(),
                venda.getDataVenda(),
                venda.getMetodoPagamento(),
                venda.getStatusVenda(),
                venda.getObservacoes()
        );
    }
}