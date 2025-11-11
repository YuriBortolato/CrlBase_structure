package com.apirest.api.service;

import com.apirest.api.dto.VendaDTO;
import com.apirest.api.dto.VendaItemDTO;
import com.apirest.api.dto.VendaItemResponseDTO;
import com.apirest.api.dto.VendaResponseDTO;
import com.apirest.api.entity.*;
import com.apirest.api.repository.ClienteRepository;
import com.apirest.api.repository.FuncionarioRepository;
import com.apirest.api.repository.ProdutoRepository;
import com.apirest.api.repository.VendaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class VendaService {
    private final VendaRepository vendaRepository;
    private final ClienteRepository clienteRepository;
    private final FuncionarioRepository funcionarioRepository;
    private final ProdutoRepository produtoRepository;

    // Definição das permissões necessárias para gerenciar vendas
    private static final Set<String> PERMISSAO_GERENCIAR_VENDA = Set.of("DONO", "GERENTE", "LIDER_VENDA", "ADMIN");

    @Transactional
    public VendaResponseDTO registrarVenda(VendaDTO dto) {

        // busca produto, validando funcionário ativo
        Funcionario funcionario = funcionarioRepository.findById(dto.getIdFuncionario())
                .orElseThrow(() -> new RuntimeException("Funcionário com ID " + dto.getIdFuncionario() + " não encontrado"));
        if (!funcionario.isAtivo()) {
            throw new RuntimeException("Funcionário " + funcionario.getNomeCompleto() + " está inativo.");
        }

        // busca produto, validando cliente ativo
        Cliente cliente = clienteRepository.findById(dto.getIdCliente())
                .orElseThrow(() -> new RuntimeException("Cliente com ID " + dto.getIdCliente() + " não encontrado"));
        if (!cliente.isAtivo()) {
            throw new RuntimeException("Cliente " + cliente.getNomeCompleto() + " está inativo.");
        }

        // Cria a venda
        Venda venda = new Venda();
        venda.setFuncionario(funcionario);
        venda.setCliente(cliente);
        venda.setMetodoPagamento(dto.getMetodoPagamento());
        venda.setObservacoes(dto.getObservacoes());
        venda.setDataVenda(LocalDateTime.now());
        venda.setStatusVenda(StatusVenda.REALIZADA);

        BigDecimal valorTotalVenda = BigDecimal.ZERO;
        List<VendaItem> itensVenda = new ArrayList<>();

        // Processa cada item da venda
        for (VendaItemDTO itemDTO : dto.getItens()) {
            Produto produto = produtoRepository.findById(itemDTO.getIdProduto())
                    .orElseThrow(() -> new RuntimeException("Produto com ID " + itemDTO.getIdProduto() + " não encontrado"));

            // Verifica estoque disponível
            if (produto.getQuantidadeEmEstoque() < itemDTO.getQuantidade()) {
                throw new RuntimeException("Estoque insuficiente para o produto: " + produto.getNome() +
                        " (Disponível: " + produto.getQuantidadeEmEstoque() + ", Pedido: " + itemDTO.getQuantidade() + ")");
            }

            // Atualiza o estoque do produto
            produto.setQuantidadeEmEstoque(produto.getQuantidadeEmEstoque() - itemDTO.getQuantidade());
            produtoRepository.save(produto); // Salva a atualização do estoque

            // Calcula subtotal
            BigDecimal precoUnitario = produto.getValorVenda(); // Usando o preço de venda do produto
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
        return toResponseDTO(vendaSalva);
    }

    // Listar todas as vendas
    public List<VendaResponseDTO> listarVendas(StatusVenda status) {
        List<Venda> vendas;
        if (status != null) {
            vendas = vendaRepository.findByStatusVenda(status);
        } else {
            vendas = vendaRepository.findAll();
        }
        return vendas.stream()
                .map(this::toResponseDTO)
                .collect(Collectors.toList());
    }

    // Buscar venda por ID
    public VendaResponseDTO buscarPorId(Long id) {
        Venda venda = findVendaById(id);
        return toResponseDTO(venda);
    }

    // Cancelar Venda (DELETE Lógico)
    @Transactional
    public VendaResponseDTO cancelarVenda(Long idVenda, Long idFuncionario) {
        validarPermissao(idFuncionario, PERMISSAO_GERENCIAR_VENDA, "cancelar venda");

        Venda venda = findVendaById(idVenda);
        if (venda.getStatusVenda() == StatusVenda.CANCELADA) {
            throw new RuntimeException("Venda " + idVenda + " já está cancelada.");
        }

        // Lógica de Estorno
        for (VendaItem item : venda.getItens()) {
            Produto produto = item.getProduto();
            // Verifica se o produto ainda existe
            if (produto != null) {
                // se o produto ainda está ativo
                if(produtoRepository.existsById(produto.getIdProduto())) {
                    produto.setQuantidadeEmEstoque(produto.getQuantidadeEmEstoque() + item.getQuantidade());
                    produtoRepository.save(produto);
                }
            }
        }
        venda.setStatusVenda(StatusVenda.CANCELADA);
        Venda vendaSalva = vendaRepository.save(venda);
        return toResponseDTO(vendaSalva);
    }

    // Reativar Venda (PATCH)
    @Transactional
    public VendaResponseDTO reativarVenda(Long idVenda, Long idFuncionario) {
        validarPermissao(idFuncionario, PERMISSAO_GERENCIAR_VENDA, "reativar venda");

        Venda venda = findVendaById(idVenda);
        if (venda.getStatusVenda() == StatusVenda.REALIZADA) {
            throw new RuntimeException("Venda " + idVenda + " já está realizada.");
        }

        // Lógica de Débito: Verifica e remove os itens do estoque NOVAMENTE
        for (VendaItem item : venda.getItens()) {
            Produto produto = item.getProduto();

            // Re-valida o produto
            if (produto == null) {
                throw new RuntimeException("Não é possível reativar: O produto associado ao item " + item.getIdVendaItem() + " não existe mais.");
            }

            // Busca o produto atualizado para garantir que está ativo
            Produto produtoAtivo = produtoRepository.findByIdProdutoAndAtivoTrue(produto.getIdProduto())
                    .orElseThrow(() -> new RuntimeException("Não é possível reativar: O produto " + produto.getNome() + " está inativo ou foi removido."));


            if (produtoAtivo.getQuantidadeEmEstoque() < item.getQuantidade()) {
                throw new RuntimeException("Não é possível reativar: Estoque insuficiente para o produto " + produtoAtivo.getNome() +
                        " (Disponível: " + produtoAtivo.getQuantidadeEmEstoque() + ", Necessário: " + item.getQuantidade() + ")");
            }

            produtoAtivo.setQuantidadeEmEstoque(produtoAtivo.getQuantidadeEmEstoque() - item.getQuantidade());
            produtoRepository.save(produtoAtivo);
        }

        venda.setStatusVenda(StatusVenda.REALIZADA);
        Venda vendaSalva = vendaRepository.save(venda);
        return toResponseDTO(vendaSalva);
    }

    // Busca Venda (ativa ou cancelada)
    private Venda findVendaById(Long id) {
        return vendaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Venda com ID " + id + " não encontrada"));
    }

    // Validação de Permissão
    private void validarPermissao(Long idFuncionario, Set<String> cargosPermitidos, String acao) {
        Funcionario funcionario = funcionarioRepository.findById(idFuncionario)
                .orElseThrow(() -> new RuntimeException("Funcionário não encontrado"));
        if (funcionario.getCargo() == null) {
            throw new RuntimeException("Funcionário com cargo nulo. Permissão negada.");
        }
        if (!cargosPermitidos.contains(funcionario.getCargo().name())) {
            throw new RuntimeException("Permissão negada: cargo (" + funcionario.getCargo().name() + ") não autorizado a " + acao);
        }
    }

    // Metodo auxiliar para converter Venda em VendaResponseDTO
    private VendaResponseDTO toResponseDTO(Venda venda) {

        // Converter os itens da venda para VendaItemResponseDTO
        List<VendaItemResponseDTO> itensDTO = venda.getItens().stream()
                .map(item -> new VendaItemResponseDTO(
                        item.getProduto().getIdProduto(),
                        item.getProduto().getNome(),
                        item.getQuantidade(),
                        item.getPrecoUnitario(),
                        item.getSubtotal()
                )).collect(Collectors.toList());

        // Retornar o VendaResponseDTO completo
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

