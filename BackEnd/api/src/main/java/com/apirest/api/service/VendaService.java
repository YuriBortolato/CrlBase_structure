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
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class VendaService {
    private final VendaRepository vendaRepository;
    private final ClienteRepository clienteRepository;
    private final FuncionarioRepository funcionarioRepository;
    private final ProdutoRepository produtoRepository;

    @Transactional
    public VendaResponseDTO registrarVenda(VendaDTO dto) {

        // busca produto, funcionário e cliente
        Funcionario funcionario = funcionarioRepository.findById(dto.getIdFuncionario())
                .orElseThrow(() -> new RuntimeException("Funcionário com ID " + dto.getIdFuncionario() + " não encontrado"));

        Cliente cliente = clienteRepository.findById(dto.getIdCliente())
                .orElseThrow(() -> new RuntimeException("Cliente com ID " + dto.getIdCliente() + " não encontrado"));

        // Cria a venda
        Venda venda = new Venda();
        venda.setFuncionario(funcionario);
        venda.setCliente(cliente);
        venda.setMetodoPagamento(dto.getMetodoPagamento());
        venda.setObservacoes(dto.getObservacoes());
        venda.setDataVenda(LocalDateTime.now());

        BigDecimal valorTotalVenda = BigDecimal.ZERO;
        List<VendaItem> itensVenda = new ArrayList<>();

        // Processa cada item da venda
        for (VendaItemDTO itemDTO : dto.getItens()) {
            Produto produto = produtoRepository.findById(itemDTO.getIdProduto())
                    .orElseThrow(() -> new RuntimeException("Produto com ID " + itemDTO.getIdProduto() + " não encontrado"));

            // Verifica estoque disponível
            if (produto.getQuantidadeEmEstoque() < itemDTO.getQuantidade()) {
                throw new RuntimeException("Estoque insuficiente para o produto: " + produto.getNome());
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
    public List<VendaResponseDTO> listarVendas() {
        return vendaRepository.findAll().stream()
                .map(this::toResponseDTO)
                .collect(Collectors.toList());
    }

    // Buscar venda por ID
    public VendaResponseDTO buscarPorId(Long id) {
        Venda venda = vendaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Venda com ID " + id + " não encontrada"));
        return toResponseDTO(venda);
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
                venda.getObservacoes()
        );
    }
}

