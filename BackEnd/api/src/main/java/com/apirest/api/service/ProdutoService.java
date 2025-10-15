package com.apirest.api.service;


import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import com.apirest.api.dto.*;
import com.apirest.api.entity.*;
import com.apirest.api.repository.*;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProdutoService {

    private final ProdutoRepository produtoRepository;
    private final CategoriaService categoriaService;
    private final FuncionarioRepository funcionarioRepository;

    // cargos permitidos para CRIAR produto
    private static final Set<Cargo> PERMISSAO_CRIAR = Set.of(Cargo.DONO, Cargo.GERENTE, Cargo.LIDER_VENDA, Cargo.ADMIN );

    // cargos permitidos para EDITAR (nome, descricao, categoria)
    private static final Set<Cargo> PERMISSAO_EDITAR = Set.of(Cargo.DONO, Cargo.GERENTE, Cargo.LIDER_VENDA, Cargo.ADMIN,
            Cargo.RECEPCIONISTA, Cargo.RECEPCIONISTA_TESTE);

    @Transactional
    public ProdutoResponseDTO criarProduto(ProdutoDTO dto) {
        // valida funcionário e permissão
        Funcionario funcionario = funcionarioRepository.findById(dto.getIdFuncionario())
                .orElseThrow(() -> new RuntimeException("Funcionário não encontrado"));
        if (!PERMISSAO_CRIAR.contains(funcionario.getCargo())) {
            throw new RuntimeException("Permissão negada: cargo não autorizado a cadastrar produto");
        }

        // normalizar nome e descrição antes de qualquer uso
        String nomeNormalizado = dto.getNome().trim().toUpperCase();
        String descricaoNormalizada = dto.getDescricao().trim().toUpperCase();

        // verificar duplicidade já com nome normalizado
        if (produtoRepository.existsByNome(nomeNormalizado)) {
            throw new RuntimeException("Produto com este nome já existe");
        }

        // Categoria: normalize e find/create
        Categoria categoria = categoriaService.findOrCreateByNameNormalize(dto.getCategoria());

        Produto produto = Produto.builder()
                .nome(dto.getNome())
                .categoria(categoria)
                .valorCusto(dto.getValorCusto())
                .valorVenda(dto.getValorVenda())
                .descricao(dto.getDescricao())
                .quantidadeEmEstoque(dto.getQuantidadeEmEstoque())
                .build();

        Produto salvo = produtoRepository.save(produto);
        return toResponseDTO(salvo);
    }

    @Transactional
    public ProdutoResponseDTO atualizarProduto(Long idProduto, ProdutoUpdateDTO dto) {
        Funcionario funcionario = funcionarioRepository.findById(dto.getIdFuncionario())
                .orElseThrow(() -> new RuntimeException("Funcionário não encontrado"));
        if (!PERMISSAO_EDITAR.contains(funcionario.getCargo())) {
            throw new RuntimeException("Permissão negada: cargo não autorizado a modificar produto");
        }

        Produto produto = produtoRepository.findById(idProduto)
                .orElseThrow(() -> new RuntimeException("Produto não encontrado"));

        // atualizar apenas nome, descricao e categoria
        produto.setNome(dto.getNome());
        produto.setDescricao(dto.getDescricao());
        Categoria categoria = categoriaService.findOrCreateByNameNormalize(dto.getCategoria());
        produto.setCategoria(categoria);

        Produto salvo = produtoRepository.save(produto);
        return toResponseDTO(salvo);
    }

    public List<ProdutoResponseDTO> listarProdutosPublico() {
        return produtoRepository.findAll().stream()
                .map(this::toResponseDTOPublicView)
                .collect(Collectors.toList());
    }

    public ProdutoResponseDTO getProdutoResponseById(Long id) {
        Produto produto = produtoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Produto não encontrado"));
        return toResponseDTO(produto);
    }


    public Produto buscarPorId(Long id) {
        return produtoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Produto não encontrado"));
    }

    // transforma produto pra DTO com status de estoque
    public ProdutoResponseDTO toResponseDTO(Produto p) {
        ProdutoResponseDTO dto = new ProdutoResponseDTO();
        dto.setIdProduto(p.getIdProduto());
        dto.setNome(p.getNome());
        dto.setDescricao(p.getDescricao());
        dto.setCategoria(p.getCategoria().getNome()); // já em maiúsculas
        dto.setValorVenda(p.getValorVenda());
        dto.setQuantidadeEmEstoque(p.getQuantidadeEmEstoque());
        dto.setStatusEstoque(statusEstoque(p.getQuantidadeEmEstoque()));
        return dto;
    }

    // visão pública (pode usar a mesma DTO, mas aqui você poderia ocultar custo, por exemplo)
    private ProdutoResponseDTO toResponseDTOPublicView(Produto p) {
        // Retorna nome, descrição, categoria, valorVenda, quantidade e status
        return toResponseDTO(p);
    }

    // regra de status de estoque
    private String statusEstoque(Integer quantidade) {
        if (quantidade == null || quantidade <= 0) return "Esgotado";
        if (quantidade >= 1 && quantidade <= 10) return "Quase Esgotado";
        return "Disponível";
    }
}
