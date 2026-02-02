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

    private final ProdutoPaiRepository produtoPaiRepository;
    private final ProdutoVariacaoRepository produtoVariacaoRepository;
    private final EstoqueSaldoRepository estoqueSaldoRepository;

    //  AUXILIARES
    private final CategoriaService categoriaService;
    private final FuncionarioRepository funcionarioRepository;

    // PERMISSÕES
    private static final Set<String> PERMISSAO_CRIAR = Set.of("DONO", "GERENTE", "LIDER_VENDA", "ADMIN");
    private static final Set<String> PERMISSAO_EDITAR = Set.of("DONO", "GERENTE", "LIDER_VENDA", "ADMIN");
    private static final Set<String> PERMISSAO_DELETAR = Set.of("DONO", "GERENTE", "LIDER_VENDA", "ADMIN");

    // CADASTRO (POST)
    @Transactional
    public void cadastrarProdutoCompleto(CadastroProdutoDTO dto) {
        validarPermissao(dto.getIdFuncionario(), PERMISSAO_CRIAR, "cadastrar produto");
        validarPrecos(dto.getVariacoes());

        Funcionario funcionario = funcionarioRepository.findById(dto.getIdFuncionario())
                .orElseThrow(() -> new RuntimeException("Funcionário não encontrado"));
        Unidade unidadeAtual = funcionario.getUnidade();
        Categoria categoria = categoriaService.findOrCreateByNameNormalize(dto.getNomeCategoria());

        // Criar ProdutoPai
        ProdutoPai pai = ProdutoPai.builder()
                .nomeGenerico(dto.getNomeGenerico().trim().toUpperCase())
                .marca(dto.getMarca())
                .ncm(dto.getNcm())
                .descricao(dto.getDescricao())
                .categoria(categoria)
                .ativo(true)
                .build();

        pai = produtoPaiRepository.save(pai);

        // Criar variações e estoques
        if (dto.getVariacoes() != null) {
            for (CadastroProdutoDTO.VariacaoDTO varDto : dto.getVariacoes()) {
                salvarVariacaoEEstoque(pai, varDto, unidadeAtual);
            }
        }
    }

    // ATUALIZAÇÃO (PUT)
    @Transactional
    public void atualizarProdutoCompleto(Long idPai, CadastroProdutoDTO dto) {
        validarPermissao(dto.getIdFuncionario(), PERMISSAO_EDITAR, "editar produto");

        ProdutoPai pai = produtoPaiRepository.findById(idPai)
                .orElseThrow(() -> new RuntimeException("Produto não encontrado"));

        validarPrecos(dto.getVariacoes());

        // Reativar se estiver inativo
        if (!pai.isAtivo()) {
            validarPermissao(dto.getIdFuncionario(), PERMISSAO_DELETAR, "reativar produto inativo");
            pai.setAtivo(true);
        }

        // Atualizar dados do ProdutoPai
        pai.setNomeGenerico(dto.getNomeGenerico().trim().toUpperCase());
        pai.setMarca(dto.getMarca());
        pai.setNcm(dto.getNcm());
        pai.setDescricao(dto.getDescricao());
        pai.setCategoria(categoriaService.findOrCreateByNameNormalize(dto.getNomeCategoria()));

        produtoPaiRepository.save(pai);

        // Atualizar ou adicionar variações
        Funcionario func = funcionarioRepository.findById(dto.getIdFuncionario()).orElseThrow();

        if (dto.getVariacoes() != null) {
            for (CadastroProdutoDTO.VariacaoDTO varDto : dto.getVariacoes()) {
                // Verificar se a variação já existe
                Optional<ProdutoVariacao> varExistente = pai.getVariacoes().stream()
                        .filter(v -> v.getNomeVariacao().equalsIgnoreCase(varDto.getNomeVariacao()))
                        .findFirst();

                if (varExistente.isPresent()) {
                    ProdutoVariacao v = varExistente.get();
                    v.setPrecoCusto(varDto.getPrecoCusto());
                    v.setPrecoVenda(varDto.getPrecoVenda());
                    v.setCodigoBarras(varDto.getCodigoBarras());
                    if(!v.isAtivo()) v.setAtivo(true);
                    produtoVariacaoRepository.save(v);
                } else {
                    salvarVariacaoEEstoque(pai, varDto, func.getUnidade());
                }
            }
        }
    }

    // DELETE LÓGICO
    @Transactional
    public void deletarProdutoLogicamente(Long idPai, Long idFuncionario) {
        validarPermissao(idFuncionario, PERMISSAO_DELETAR, "deletar produto");

        ProdutoPai pai = produtoPaiRepository.findById(idPai)
                .orElseThrow(() -> new RuntimeException("Produto não encontrado"));

        if (!pai.isAtivo()) throw new RuntimeException("Produto já está inativo.");

        pai.setAtivo(false);
        // Desativar todas as variações associadas
        pai.getVariacoes().forEach(v -> v.setAtivo(false));

        produtoPaiRepository.save(pai);
    }

    // LISTAGEM (GET)
    public List<ProdutoResponseDTO> listarTudo(Long idFuncionarioSolicitante) {
        Long idUnidadeAlvo = null;
        if (idFuncionarioSolicitante != null) {
            Funcionario f = funcionarioRepository.findById(idFuncionarioSolicitante).orElse(null);
            if (f != null) idUnidadeAlvo = f.getUnidade().getIdUnidade();
        }
        Long finalIdUnidade = idUnidadeAlvo;

        // Filtrar apenas produtos ativos
        return produtoPaiRepository.findAll().stream()
                .filter(ProdutoPai::isAtivo)
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

    // AUXILIARES

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
        variacao.setSku("SKU-" + System.currentTimeMillis() + "-" + (int)(Math.random()*1000)); // SKU Gerado

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
        List<VariacaoResponseDTO> variacoesDTO = pai.getVariacoes().stream()
                .filter(ProdutoVariacao::isAtivo)
                .map(v -> {
                    Integer qtd = 0;
                    Integer min = 5;

                    // Buscar saldo se idUnidade for fornecido
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

    // CÁLCULO STATUS ESTOQUE
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