package com.apirest.api.service;


import com.apirest.api.dto.*;
import com.apirest.api.entity.Cargo;
import com.apirest.api.entity.Categoria;
import com.apirest.api.entity.Funcionario;
import com.apirest.api.entity.Produto;
import com.apirest.api.repository.FuncionarioRepository;
import com.apirest.api.repository.ProdutoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProdutoService {

    private final ProdutoRepository produtoRepository;
    private final CategoriaService categoriaService;
    private final FuncionarioRepository funcionarioRepository;

    private static final Set<String> PERMISSAO_CRIAR = Set.of("DONO", "GERENTE", "LIDER_VENDA", "ADMIN");
    private static final Set<String> PERMISSAO_EDITAR_INFO = Set.of("DONO", "GERENTE", "LIDER_VENDA", "ADMIN", "RECEPCIONISTA");
    private static final Set<String> PERMISSAO_EDITAR_PRECO_ESTOQUE_DESC = Set.of("DONO", "GERENTE", "LIDER_VENDA", "ADMIN");
    private static final Set<String> PERMISSAO_DELETAR = Set.of("DONO", "GERENTE", "ADMIN");

    @Transactional
    public ProdutoResponseDTO criarProduto(ProdutoDTO dto) {
        validarPermissao(dto.getIdFuncionario(), PERMISSAO_CRIAR, "cadastrar produto");

        if (dto.getValorCusto().compareTo(dto.getValorVenda()) >= 0) {
            throw new RuntimeException("O valor de venda deve ser maior que o valor de custo.");
        }

        String nomeNormalizado = dto.getNome().trim().toUpperCase();
        if (produtoRepository.existsByNomeAndAtivoTrue(nomeNormalizado)) {
            throw new RuntimeException("Produto com este nome já existe");
        }

        Categoria categoria = categoriaService.findOrCreateByNameNormalize(dto.getCategoria());

        Produto produto = Produto.builder()
                .nome(nomeNormalizado)
                .categoria(categoria)
                .valorCusto(dto.getValorCusto())
                .valorVenda(dto.getValorVenda())
                .descricao(dto.getDescricao())
                .quantidadeEmEstoque(dto.getQuantidadeEmEstoque())
                .ativo(true)
                .build();

        Produto salvo = produtoRepository.save(produto);
        return toResponseDTO(salvo);
    }

    @Transactional
    public ProdutoResponseDTO atualizarInformacoes(Long idProduto, ProdutoUpdateDTO dto) {
        validarPermissao(dto.getIdFuncionario(), PERMISSAO_EDITAR_INFO, "modificar informações do produto");

        if (dto.getValorCusto().compareTo(dto.getValorVenda()) >= 0) {
            throw new RuntimeException("O valor de venda deve ser maior que o valor de custo.");
        }

        Produto produto = findProdutoAtivoById(idProduto);

        produto.setNome(dto.getNome().trim().toUpperCase());
        produto.setDescricao(dto.getDescricao());
        Categoria categoria = categoriaService.findOrCreateByNameNormalize(dto.getCategoria());
        produto.setCategoria(categoria);

        produto.setValorCusto(dto.getValorCusto());
        produto.setValorVenda(dto.getValorVenda());
        produto.setQuantidadeEmEstoque(dto.getQuantidadeEmEstoque());

        Produto salvo = produtoRepository.save(produto);
        return toResponseDTO(salvo);
    }

    @Transactional
    public ProdutoResponseDTO atualizarPrecoEEstoque(Long idProduto, ProdutoPrecoEstoqueUpdateDTO dto) {
        validarPermissao(dto.getIdFuncionario(), PERMISSAO_EDITAR_PRECO_ESTOQUE_DESC, "modificar preço, estoque ou descrição");
        if (dto.getValorCusto().compareTo(dto.getValorVenda()) >= 0) {
            throw new RuntimeException("O valor de venda deve ser maior que o valor de custo.");
        }

        Produto produto = findProdutoAtivoById(idProduto);
        produto.setValorCusto(dto.getValorCusto());
        produto.setValorVenda(dto.getValorVenda());
        produto.setQuantidadeEmEstoque(dto.getQuantidadeEmEstoque());
        produto.setDescricao(dto.getDescricao());

        Produto salvo = produtoRepository.save(produto);
        return toResponseDTO(salvo);
    }

    @Transactional
    public void deletarProdutoLogicamente(Long idProduto, Long idFuncionario) {
        validarPermissao(idFuncionario, PERMISSAO_DELETAR, "deletar produto");
        Produto produto = findProdutoAtivoById(idProduto);
        produto.setAtivo(false);
        produtoRepository.save(produto);
    }

    public List<ProdutoResponseDTO> listarProdutosAtivos() {
        return produtoRepository.findAllByAtivoTrue().stream()
                .map(this::toResponseDTO)
                .collect(Collectors.toList());
    }

    public ProdutoResponseDTO getProdutoResponseById(Long id) {
        Produto produto = findProdutoAtivoById(id);
        return toResponseDTO(produto);
    }

    private Produto findProdutoAtivoById(Long id) {
        return produtoRepository.findByIdProdutoAndAtivoTrue(id)
                .orElseThrow(() -> new RuntimeException("Produto não encontrado ou inativo. ID: " + id));
    }

    private void validarPermissao(Long idFuncionario, Set<String> cargosPermitidos, String acao) {
        Funcionario funcionario = funcionarioRepository.findById(idFuncionario)
                .orElseThrow(() -> new RuntimeException("Funcionário não encontrado"));
        // Verifica se o cargo do funcionário está na lista de cargos permitidos
        if (!cargosPermitidos.contains(funcionario.getCargo().name())) {
            throw new RuntimeException("Permissão negada: cargo não autorizado a " + acao);
        }
    }

    private ProdutoResponseDTO toResponseDTO(Produto p) {
        ProdutoResponseDTO dto = new ProdutoResponseDTO();
        dto.setIdProduto(p.getIdProduto());
        dto.setNome(p.getNome());
        dto.setDescricao(p.getDescricao());
        dto.setCategoria(p.getCategoria().getNome());
        dto.setValorVenda(p.getValorVenda());
        dto.setQuantidadeEmEstoque(p.getQuantidadeEmEstoque());
        dto.setStatusEstoque(statusEstoque(p.getQuantidadeEmEstoque()));
        dto.setAtivo(p.isAtivo());
        return dto;
    }
        // Define o status do estoque com base na quantidade
        private String statusEstoque(Integer quantidade) {
            if (quantidade == null || quantidade <= 0) {
                return "Esgotado";
            }
            if (quantidade <= 10) {
                return "Quase Esgotado";
            }
            return "Disponível";
        }
}
