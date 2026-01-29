package com.apirest.api.service;

import com.apirest.api.dto.*;
import com.apirest.api.entity.*;
import com.apirest.api.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProdutoService {

    // Repositórios da Nova Arquitetura
    private final ProdutoPaiRepository produtoPaiRepository;
    private final ProdutoVariacaoRepository produtoVariacaoRepository;
    private final EstoqueSaldoRepository estoqueSaldoRepository;

    // Auxiliares
    private final CategoriaService categoriaService;
    private final FuncionarioRepository funcionarioRepository;

    // --- REGRAS DE PERMISSÃO  ---
    private static final Set<String> PERMISSAO_CRIAR = Set.of("DONO", "GERENTE", "LIDER_VENDA", "ADMIN");
    private static final Set<String> PERMISSAO_EDITAR = Set.of("DONO", "GERENTE", "LIDER_VENDA", "ADMIN");
    private static final Set<String> PERMISSAO_DELETAR = Set.of("DONO", "GERENTE", "LIDER_VENDA", "ADMIN");

    // --- CADASTRO (POST) ---
    @Transactional
    public void cadastrarProdutoCompleto(CadastroProdutoDTO dto) {
        // Validação de permissão
        validarPermissao(dto.getIdFuncionario(), PERMISSAO_CRIAR, "cadastrar produto");

        Funcionario funcionario = funcionarioRepository.findById(dto.getIdFuncionario())
                .orElseThrow(() -> new RuntimeException("Funcionário não encontrado"));
        Unidade unidadeAtual = funcionario.getUnidade();

        // Validação de preços
        validarPrecos(dto.getVariacoes());

        // Normalização do nome genérico
        String nomeNormalizado = dto.getNomeGenerico().trim().toUpperCase();


        // Busca ou cria categoria
        Categoria categoria = categoriaService.findOrCreateByNameNormalize(dto.getNomeCategoria());

        // Criação do Produto Pai
        ProdutoPai pai = ProdutoPai.builder()
                .nomeGenerico(nomeNormalizado)
                .marca(dto.getMarca())
                .ncm(dto.getNcm())
                .descricao(dto.getDescricao())
                .categoria(categoria)
                .ativo(true)
                .build();

        pai = produtoPaiRepository.save(pai);

       // Criação das Variações e Estoques
        if (dto.getVariacoes() != null) {
            for (CadastroProdutoDTO.VariacaoDTO varDto : dto.getVariacoes()) {
                salvarVariacaoEEstoque(pai, varDto, unidadeAtual);
            }
        }
    }

    // --- ATUALIZAÇÃO COMPLETA (PUT) ---
    @Transactional
    public void atualizarProdutoCompleto(Long idPai, CadastroProdutoDTO dto) {
        validarPermissao(dto.getIdFuncionario(), PERMISSAO_EDITAR, "editar produto");

        ProdutoPai pai = produtoPaiRepository.findById(idPai)
                .orElseThrow(() -> new RuntimeException("Produto não encontrado"));

        validarPrecos(dto.getVariacoes());

        // --- LÓGICA DE REATIVAÇÃO ---
        if (!pai.isAtivo()) {
            // Reativação
            validarPermissao(dto.getIdFuncionario(), PERMISSAO_DELETAR, "reativar produto inativo");
            pai.setAtivo(true);
        }

        // Atualiza dados básicos
        pai.setNomeGenerico(dto.getNomeGenerico().trim().toUpperCase());
        pai.setMarca(dto.getMarca());
        pai.setNcm(dto.getNcm());
        pai.setDescricao(dto.getDescricao());
        pai.setCategoria(categoriaService.findOrCreateByNameNormalize(dto.getNomeCategoria()));

        produtoPaiRepository.save(pai);

        // Atualiza ou adiciona variações
        Funcionario func = funcionarioRepository.findById(dto.getIdFuncionario()).orElseThrow();
        if (dto.getVariacoes() != null) {
            for (CadastroProdutoDTO.VariacaoDTO varDto : dto.getVariacoes()) {
                // Tenta encontrar variação existente pelo nome
                Optional<ProdutoVariacao> varExistente = pai.getVariacoes().stream()
                        .filter(v -> v.getNomeVariacao().equalsIgnoreCase(varDto.getNomeVariacao()))
                        .findFirst();

                if (varExistente.isPresent()) {
                    // Atualiza preço
                    ProdutoVariacao v = varExistente.get();
                    v.setPrecoCusto(varDto.getPrecoCusto());
                    v.setPrecoVenda(varDto.getPrecoVenda());
                    v.setCodigoBarras(varDto.getCodigoBarras());
                    produtoVariacaoRepository.save(v);
                } else {
                    // Nova variação
                    salvarVariacaoEEstoque(pai, varDto, func.getUnidade());
                }
            }
        }
    }

    // --- DELETE LÓGICO  ---
    @Transactional
    public void deletarProdutoLogicamente(Long idPai, Long idFuncionario) {
        validarPermissao(idFuncionario, PERMISSAO_DELETAR, "deletar produto");

        ProdutoPai pai = produtoPaiRepository.findById(idPai)
                .orElseThrow(() -> new RuntimeException("Produto não encontrado"));

        if (!pai.isAtivo()) {
            throw new RuntimeException("Produto já está inativo.");
        }

        pai.setAtivo(false);
        pai.getVariacoes().forEach(v -> v.setAtivo(false));

        produtoPaiRepository.save(pai);
    }

    // --- LISTAGEM (GET) ---
    public List<ProdutoResponseDTO> listarTudo(Long idFuncionarioSolicitante) {
        Long idUnidadeAlvo = null;
        if (idFuncionarioSolicitante != null) {
            Funcionario f = funcionarioRepository.findById(idFuncionarioSolicitante).orElse(null);
            if (f != null) idUnidadeAlvo = f.getUnidade().getIdUnidade();
        }
        Long finalIdUnidade = idUnidadeAlvo;

        // Busca todos os produtos pais
        List<ProdutoPai> lista = produtoPaiRepository.findAll(); // Assumindo que busca tudo

        return lista.stream()
                .filter(ProdutoPai::isAtivo) // Filtra apenas ativos
                .map(pai -> montarDTOResposta(pai, finalIdUnidade))
                .collect(Collectors.toList());
    }

    public ProdutoResponseDTO buscarPorId(Long id, Long idFuncionario) {
        ProdutoPai pai = produtoPaiRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Produto não encontrado"));

        Long idUnidade = null;
        if(idFuncionario != null){
            Funcionario f = funcionarioRepository.findById(idFuncionario).orElse(null);
            if(f != null) idUnidade = f.getUnidade().getIdUnidade();
        }

        return montarDTOResposta(pai, idUnidade);
    }

    // --- MÉTODOS AUXILIARES ---

    private void salvarVariacaoEEstoque(ProdutoPai pai, CadastroProdutoDTO.VariacaoDTO varDto, Unidade unidade) {
        ProdutoVariacao variacao = ProdutoVariacao.builder()
                .produtoPai(pai)
                .nomeVariacao(varDto.getNomeVariacao())
                .precoCusto(varDto.getPrecoCusto())
                .precoVenda(varDto.getPrecoVenda())
                .codigoBarras(varDto.getCodigoBarras())
                .ativo(true)
                .build();

        variacao.setNomeCompletoConcatenado(pai.getNomeGenerico() + " - " + varDto.getNomeVariacao());
        variacao.setSku("SKU-" + System.currentTimeMillis() + "-" + (int)(Math.random()*1000));

        variacao = produtoVariacaoRepository.save(variacao);

        EstoqueSaldo saldo = EstoqueSaldo.builder()
                .unidade(unidade)
                .produtoVariacao(variacao)
                .quantidadeAtual(varDto.getEstoqueInicial() != null ? varDto.getEstoqueInicial() : 0)
                .quantidadeMinima(varDto.getEstoqueMinimo() != null ? varDto.getEstoqueMinimo() : 5)
                .build();
        estoqueSaldoRepository.save(saldo);
    }

    private ProdutoResponseDTO montarDTOResposta(ProdutoPai pai, Long idUnidade) {
        List<VariacaoResponseDTO> variacoesDTO = pai.getVariacoes().stream().map(v -> {
            Integer qtd = 0;
            Integer min = 5;
            // Busca saldo na unidade específica
            if (idUnidade != null) {
                Optional<EstoqueSaldo> saldoOpt = estoqueSaldoRepository.findByUnidadeIdAndProdutoVariacaoId(idUnidade, v.getId());
                if (saldoOpt.isPresent()) {
                    qtd = saldoOpt.get().getQuantidadeAtual();
                    min = saldoOpt.get().getQuantidadeMinima();
                }
            }

            return VariacaoResponseDTO.builder()
                    .id(v.getId())
                    .nomeVariacao(v.getNomeVariacao())
                    .nomeCompleto(v.getNomeCompletoConcatenado())
                    .sku(v.getSku())
                    .codigoBarras(v.getCodigoBarras())
                    .precoCusto(v.getPrecoCusto())
                    .precoVenda(v.getPrecoVenda())
                    .estoqueAtual(qtd)
                    .statusEstoque(calcularStatusEstoque(qtd, min))
                    .ativo(v.isAtivo())
                    .build();
        }).collect(Collectors.toList());

        return ProdutoResponseDTO.builder()
                .id(pai.getId())
                .nomeGenerico(pai.getNomeGenerico())
                .marca(pai.getMarca())
                .descricao(pai.getDescricao())
                .ncm(pai.getNcm())
                .categoria(pai.getCategoria().getNome())
                .ativo(pai.isAtivo())
                .variacoes(variacoesDTO)
                .build();
    }

    //
    private String calcularStatusEstoque(Integer qtd, Integer min) {
        if (qtd == null || qtd <= 0) return "Esgotado";
        int minimo = (min != null && min >= 0) ? min : 5;
        if (qtd <= minimo) return "Quase Esgotado";
        return "Disponível";
    }

    private void validarPermissao(Long idFuncionario, Set<String> cargos, String acao) {
        if(idFuncionario == null) return;
        Funcionario f = funcionarioRepository.findById(idFuncionario)
                .orElseThrow(() -> new RuntimeException("Funcionário não encontrado"));
        if (!cargos.contains(f.getCargo().name())) {
            throw new RuntimeException("Permissão negada: seu cargo não permite " + acao);
        }
    }

    private void validarPrecos(List<CadastroProdutoDTO.VariacaoDTO> variacoes) {
        if(variacoes == null) return;
        for (CadastroProdutoDTO.VariacaoDTO v : variacoes) {
            if (v.getPrecoCusto().compareTo(v.getPrecoVenda()) >= 0) {
                throw new RuntimeException("Erro no item '" + v.getNomeVariacao() + "': Preço de venda deve ser maior que o custo.");
            }
        }
    }
}