package com.apirest.api.service;

import com.apirest.api.dto.*;
import com.apirest.api.entity.*;
import com.apirest.api.repository.CaixaMovimentacaoRepository;
import com.apirest.api.repository.CaixaRepository;
import com.apirest.api.repository.FuncionarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.apirest.api.entity.FiltroPeriodo;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.temporal.TemporalField;
import java.time.temporal.WeekFields;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class CaixaService {

    private final CaixaRepository caixaRepository;
    private final FuncionarioRepository funcionarioRepository;
    private final CaixaMovimentacaoRepository caixaMovimentacaoRepository;

    // --- DEFINIÇÃO DOS CARGOS ---
    private static final Set<Cargo> PERMISSAO_CARGO_ALTO = Set.of(
            Cargo.DONO, Cargo.GERENTE, Cargo.LIDER_VENDA, Cargo.ADMIN
    );

    // Abertura de Caixa
    @Transactional
    public Caixa abrirCaixa(CaixaAberturaDTO dto) {
        log.info("Tentativa de abertura de caixa para funcionário ID: {}", dto.getIdFuncionario());

        Funcionario funcionario = funcionarioRepository.findById(dto.getIdFuncionario())
                .orElseThrow(() -> new RuntimeException("Funcionário não encontrado"));

        if (caixaRepository.existsByFuncionarioAndStatus(funcionario, StatusCaixa.ABERTO)) {
            log.warn("Bloqueio: Funcionário {} tentou abrir novo caixa com um já aberto.", funcionario.getNomeCompleto());
            throw new RuntimeException("Você já possui um caixa aberto. Feche-o antes de iniciar um novo.");
        }

        Caixa caixa = Caixa.builder()
                .funcionario(funcionario)
                .saldoInicial(dto.getSaldoInicial())
                .observacoes(dto.getObservacao())
                .status(StatusCaixa.ABERTO)
                .build();

        return caixaRepository.save(caixa);
    }

    // Fechamento de Caixa
    @Transactional
    public Caixa fecharCaixa(Long idCaixa, CaixaFechamentoDTO dto) {
        log.info("Tentativa de fechamento de caixa ID: {}", idCaixa);

        Caixa caixa = caixaRepository.findById(idCaixa)
                .orElseThrow(() -> new RuntimeException("Caixa não encontrado"));

        if (caixa.getStatus() == StatusCaixa.FECHADO) {
            throw new RuntimeException("Este caixa já está fechado.");
        }

        // Calcular total do SISTEMA (Vendas)
        BigDecimal totalVendasSistema = BigDecimal.ZERO;
        for (Venda v : caixa.getVendas()) {
            if (v.getStatusVenda() == StatusVenda.REALIZADA) {
                totalVendasSistema = totalVendasSistema.add(v.getValorTotal());
            }
        }

        // Calcular total INFORMADO (Conferido pelo funcionário)
        BigDecimal totalInformado = dto.getDinheiro()
                .add(dto.getTransferencia())
                .add(dto.getDebito())
                .add(dto.getCredito())
                .add(dto.getCrediario());

        // Calcular suprimentos e sangrias
        BigDecimal totalSuprimentos = caixaMovimentacaoRepository.somarPorTipo(caixa, CaixaMovimentacao.TipoMovimentacao.SUPRIMENTO);
        BigDecimal totalSangrias = caixaMovimentacaoRepository.somarPorTipo(caixa, CaixaMovimentacao.TipoMovimentacao.SANGRIA);

        if (totalSuprimentos == null) totalSuprimentos = BigDecimal.ZERO;
        if (totalSangrias == null) totalSangrias = BigDecimal.ZERO;

        BigDecimal esperado = caixa.getSaldoInicial()
                .add(totalVendasSistema)
                .add(totalSuprimentos)
                .subtract(totalSangrias);        BigDecimal quebra = totalInformado.subtract(esperado);

        // Atualiza Entidade
        caixa.setConferidoDinheiro(dto.getDinheiro());
        caixa.setConferidoPix(dto.getTransferencia());
        caixa.setConferidoDebito(dto.getDebito());
        caixa.setConferidoCredito(dto.getCredito());
        caixa.setConferidoCrediario(dto.getCrediario());

        caixa.setSistemaTotalVendas(totalVendasSistema);
        caixa.setQuebraDeCaixa(quebra);

        caixa.setDataFechamento(LocalDateTime.now(ZoneId.of("America/Sao_Paulo")));
        caixa.setStatus(StatusCaixa.FECHADO);

        if (dto.getObservacao() != null) {
            caixa.setObservacoes(caixa.getObservacoes() + " | Fechamento: " + dto.getObservacao());
        }

        log.info("Caixa {} fechado. Quebra calculada: {}", idCaixa, quebra);
        return caixaRepository.save(caixa);
    }

    @Transactional(readOnly = true)
    public CaixaResponseDTO buscarPorId(Long id) {
        Caixa caixa = caixaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Caixa não encontrado com ID: " + id));
        return toResponseDTO(caixa); // Certifique-se de ter o método toResponseDTO
    }

    // OTIMIZAÇÃO: readOnly = true melhora performance de consultas
    @Transactional(readOnly = true)
    public List<Caixa> listarComFiltros(Long idFuncionario, StatusCaixa status, LocalDateTime inicio, LocalDateTime fim) {
        return caixaRepository.findByFiltros(idFuncionario, status, inicio, fim);
    }

    // RELATÓRIO AVANÇADO (Com filtros complexos)
    @Transactional(readOnly = true)
    public Map<String, Object> gerarRelatorioAvancado(
            Long idSolicitante,
            Long idFuncionarioAlvo,
            FiltroPeriodo periodo,
            StatusCaixa status,
            LocalDate dataInicio,
            LocalDate dataFim
    ) {
        // Validar Solicitante
        Funcionario solicitante = funcionarioRepository.findById(idSolicitante)
                .orElseThrow(() -> new RuntimeException("Solicitante não encontrado"));

        // Regra de Permissão (RBAC) usando a Constante
        boolean isAltoEscalao = PERMISSAO_CARGO_ALTO.contains(solicitante.getCargo());
        Long idFiltroFinal = idFuncionarioAlvo;

        if (!isAltoEscalao) {
            if (idFuncionarioAlvo != null && !idFuncionarioAlvo.equals(solicitante.getIdFuncionario())) {
                throw new RuntimeException("ACESSO NEGADO: Você não tem permissão para ver relatórios de outros funcionários.");
            }
            idFiltroFinal = solicitante.getIdFuncionario();
        }

        // Cálculo de Datas
        LocalDateTime inicio;
        LocalDateTime fim;
        LocalDate hoje = LocalDate.now();
        TemporalField fieldISO = WeekFields.of(Locale.getDefault()).dayOfWeek();

        if (periodo == null) periodo = FiltroPeriodo.HOJE; // default

        switch (periodo) {
            case ONTEM -> {
                inicio = hoje.minusDays(1).atStartOfDay();
                fim = hoje.minusDays(1).atTime(LocalTime.MAX);
            }
            case ESTA_SEMANA -> { // Do primeiro dia desta semana até agora
                inicio = hoje.with(fieldISO, 1).atStartOfDay();
                fim = hoje.with(fieldISO, 7).atTime(LocalTime.MAX);
            }
            case SEMANA_PASSADA -> { // Semana anterior completa
                LocalDate semanaPassada = hoje.minusWeeks(1);
                inicio = semanaPassada.with(fieldISO, 1).atStartOfDay();
                fim = semanaPassada.with(fieldISO, 7).atTime(LocalTime.MAX);
            }
            case ESTE_MES -> { // Do dia 1 até o último dia deste mês
                inicio = hoje.withDayOfMonth(1).atStartOfDay();
                fim = hoje.withDayOfMonth(hoje.lengthOfMonth()).atTime(LocalTime.MAX);
            }
            case MES_PASSADO -> { // Mês anterior completo
                LocalDate mesPassado = hoje.minusMonths(1);
                inicio = mesPassado.withDayOfMonth(1).atStartOfDay();
                fim = mesPassado.withDayOfMonth(mesPassado.lengthOfMonth()).atTime(LocalTime.MAX);
            }
            case CUSTOM -> {
                if (dataInicio == null || dataFim == null) {
                    throw new RuntimeException("Para filtro CUSTOM, dataInicio e dataFim são obrigatórios.");
                }
                inicio = dataInicio.atStartOfDay();
                fim = dataFim.atTime(LocalTime.MAX);
            }
            default -> { // HOJE
                inicio = hoje.atStartOfDay();
                fim = hoje.atTime(LocalTime.MAX);
            }
        }
        // Buscar Caixas com os Filtros Aplicados
        List<Caixa> caixas = caixaRepository.findByFiltros(idFiltroFinal, status, inicio, fim);
        DashboardResumoDTO resumo = calcularSomaDeCaixas(caixas);
        List<CaixaResponseDTO> listaDTOs = caixas.stream().map(this::toResponseDTO).toList();

        // Montar Resposta
        Map<String, Object> response = new HashMap<>();
        response.put("periodoDescricao", periodo.toString());
        response.put("dataInicio", inicio);
        response.put("dataFim", fim);
        response.put("filtrosAplicados", Map.of(
                "visualizandoFuncionario", (idFiltroFinal == null ? "TODOS" : idFiltroFinal),
                "solicitadoPor", solicitante.getNomeCompleto(),
                "cargoSolicitante", solicitante.getCargo()
        ));
        response.put("resumo", resumo);
        response.put("listaCaixas", listaDTOs);

        return response;
    }

    // Resumo Dashboard
    public DashboardResumoDTO calcularResumoDashboard(List<Caixa> caixasFiltrados) {
        return calcularSomaDeCaixas(caixasFiltrados);
    }

    // Relatório Individual
    @Transactional(readOnly = true)
    public RelatorioIndividualDTO gerarRelatorioIndividual(Long idFuncionario) {
        Funcionario funcionario = funcionarioRepository.findById(idFuncionario)
                .orElseThrow(() -> new RuntimeException("Funcionário não encontrado"));

        LocalDateTime agora = LocalDateTime.now(ZoneId.of("America/Sao_Paulo"));

        LocalDateTime inicioDia = agora.toLocalDate().atStartOfDay();
        LocalDateTime fimDia = agora.toLocalDate().atTime(LocalTime.MAX);

        LocalDateTime inicioMes = agora.withDayOfMonth(1).toLocalDate().atStartOfDay();
        LocalDateTime fimMes = agora.toLocalDate().atTime(LocalTime.MAX);

        List<Caixa> caixasHoje = caixaRepository.findCaixasFechadosPorPeriodo(idFuncionario, inicioDia, fimDia);
        List<Caixa> caixasMes = caixaRepository.findCaixasFechadosPorPeriodo(idFuncionario, inicioMes, fimMes);

        DashboardResumoDTO resumoHoje = calcularSomaDeCaixas(caixasHoje);
        DashboardResumoDTO resumoMes = calcularSomaDeCaixas(caixasMes);

        return RelatorioIndividualDTO.builder()
                .nomeFuncionario(funcionario.getNomeCompleto())
                .totalGeralHoje(resumoHoje.getTotalConferido())
                .dinheiroHoje(resumoHoje.getTotalDinheiro())
                .pixHoje(resumoHoje.getTotalPix())
                .debitoHoje(resumoHoje.getTotalDebito())
                .creditoHoje(resumoHoje.getTotalCredito())
                .crediarioHoje(resumoHoje.getTotalCrediario())
                .totalGeralMes(resumoMes.getTotalConferido())
                .dinheiroMes(resumoMes.getTotalDinheiro())
                .pixMes(resumoMes.getTotalPix())
                .debitoMes(resumoMes.getTotalDebito())
                .creditoMes(resumoMes.getTotalCredito())
                .crediarioMes(resumoMes.getTotalCrediario())
                .build();
    }

    // Conversão para DTO
    public CaixaResponseDTO toResponseDTO(Caixa c) {
        return CaixaResponseDTO.builder()
                .idCaixa(c.getIdCaixa())
                .idFuncionario(c.getFuncionario().getIdFuncionario())
                .nomeFuncionario(c.getFuncionario().getNomeCompleto())
                .dataAbertura(c.getDataAbertura())
                .dataFechamento(c.getDataFechamento())
                .status(c.getStatus())
                .saldoInicial(c.getSaldoInicial())
                .conferidoDinheiro(c.getConferidoDinheiro())
                .conferidoPix(c.getConferidoPix())
                .conferidoDebito(c.getConferidoDebito())
                .conferidoCredito(c.getConferidoCredito())
                .conferidoCrediario(c.getConferidoCrediario())
                .quebraDeCaixa(c.getQuebraDeCaixa())
                .observacoes(c.getObservacoes())
                .build();
    }

    // Relatório Global (Dia)
    @Transactional(readOnly = true)
    public RelatorioPeriodoDTO gerarRelatorioDoDia(LocalDate data) {
        LocalDateTime inicio = data.atStartOfDay();
        LocalDateTime fim = data.atTime(LocalTime.MAX);

        List<Caixa> caixas = caixaRepository.findAllCaixasFechadosPorPeriodo(inicio, fim);
        DashboardResumoDTO soma = calcularSomaDeCaixas(caixas);

        return RelatorioPeriodoDTO.builder()
                .periodo(data.toString())
                .totalGeral(soma.getTotalConferido())
                .totalDinheiro(soma.getTotalDinheiro())
                .totalPix(soma.getTotalPix())
                .totalDebito(soma.getTotalDebito())
                .totalCredito(soma.getTotalCredito())
                .totalCrediario(soma.getTotalCrediario())
                .quantidadeCaixasFechados(caixas.size())
                .build();
    }

    // Relatório Global (Mês)
    @Transactional(readOnly = true)
    public RelatorioPeriodoDTO gerarRelatorioDoMes(int ano, int mes) {
        LocalDate dataInicial = LocalDate.of(ano, mes, 1);
        LocalDateTime inicio = dataInicial.atStartOfDay();
        LocalDateTime fim = dataInicial.withDayOfMonth(dataInicial.lengthOfMonth()).atTime(LocalTime.MAX);

        List<Caixa> caixas = caixaRepository.findAllCaixasFechadosPorPeriodo(inicio, fim);
        DashboardResumoDTO soma = calcularSomaDeCaixas(caixas);

        return RelatorioPeriodoDTO.builder()
                .periodo(mes + "/" + ano)
                .totalGeral(soma.getTotalConferido())
                .totalDinheiro(soma.getTotalDinheiro())
                .totalPix(soma.getTotalPix())
                .totalDebito(soma.getTotalDebito())
                .totalCredito(soma.getTotalCredito())
                .totalCrediario(soma.getTotalCrediario())
                .quantidadeCaixasFechados(caixas.size())
                .build();
    }

    // --- MÉTODOS AUXILIARES PRIVADOS ---

    private DashboardResumoDTO calcularSomaDeCaixas(List<Caixa> listaCaixas) {
        // Conferido (Manual)
        BigDecimal dinheiro = BigDecimal.ZERO;
        BigDecimal pix = BigDecimal.ZERO;
        BigDecimal debito = BigDecimal.ZERO;
        BigDecimal credito = BigDecimal.ZERO;
        BigDecimal crediario = BigDecimal.ZERO;

        // Previsto (Sistema)
        BigDecimal sisDinheiro = BigDecimal.ZERO;
        BigDecimal sisPix = BigDecimal.ZERO;
        BigDecimal sisDebito = BigDecimal.ZERO;
        BigDecimal sisCredito = BigDecimal.ZERO;
        BigDecimal sisCrediario = BigDecimal.ZERO;

        BigDecimal previstoSistemaTotal = BigDecimal.ZERO;
        BigDecimal quebraTotal = BigDecimal.ZERO;

        for (Caixa c : listaCaixas) {
            // Soma Conferido
            if (c.getConferidoDinheiro() != null) dinheiro = dinheiro.add(c.getConferidoDinheiro());
            if (c.getConferidoPix() != null) pix = pix.add(c.getConferidoPix());
            if (c.getConferidoDebito() != null) debito = debito.add(c.getConferidoDebito());
            if (c.getConferidoCredito() != null) credito = credito.add(c.getConferidoCredito());
            if (c.getConferidoCrediario() != null) crediario = crediario.add(c.getConferidoCrediario());
            if (c.getQuebraDeCaixa() != null) quebraTotal = quebraTotal.add(c.getQuebraDeCaixa());

            // Soma Previsto (Itera sobre as vendas deste caixa)
            if (c.getVendas() != null) {
                for (Venda v : c.getVendas()) {
                    if (v.getStatusVenda() == StatusVenda.REALIZADA) {
                        previstoSistemaTotal = previstoSistemaTotal.add(v.getValorTotal());

                        switch (v.getMetodoPagamento()) {
                            case DINHEIRO -> sisDinheiro = sisDinheiro.add(v.getValorTotal());
                            case PIX -> sisPix = sisPix.add(v.getValorTotal());
                            case DEBITO -> sisDebito = sisDebito.add(v.getValorTotal());
                            case CREDITO -> sisCredito = sisCredito.add(v.getValorTotal());
                            case CREDIARIO -> sisCrediario = sisCrediario.add(v.getValorTotal());
                        }
                    }
                }
            }
        }

        BigDecimal totalConferidoGeral = dinheiro.add(pix).add(debito).add(credito).add(crediario);

        return DashboardResumoDTO.builder()
                // Totais Gerais
                .totalPrevisto(previstoSistemaTotal)
                .totalConferido(totalConferidoGeral)
                .totalQuebra(quebraTotal)

                // Detalhes Conferidos
                .totalDinheiro(dinheiro)
                .totalPix(pix)
                .totalDebito(debito)
                .totalCredito(credito)
                .totalCrediario(crediario)

                // NOVO: Detalhes Previstos
                .previstoDinheiro(sisDinheiro)
                .previstoPix(sisPix)
                .previstoDebito(sisDebito)
                .previstoCredito(sisCredito)
                .previstoCrediario(sisCrediario)
                .build();
    }

    // Adicionar Movimentação (Sangria/Suprimento)
    @Transactional
    public CaixaMovimentacao adicionarMovimentacao(Long idCaixa, String tipo, BigDecimal valor, String motivo) {
        Caixa caixa = caixaRepository.findById(idCaixa)
                .orElseThrow(() -> new RuntimeException("Caixa não encontrado"));

        if (caixa.getStatus() != StatusCaixa.ABERTO) {
            throw new RuntimeException("Só é possível movimentar caixas ABERTOS.");
        }

        // Converte string para Enum (SANGRIA ou SUPRIMENTO)
        CaixaMovimentacao.TipoMovimentacao tipoEnum;
        try {
            tipoEnum = CaixaMovimentacao.TipoMovimentacao.valueOf(tipo.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Tipo de movimentação inválido. Use 'SANGRIA' ou 'SUPRIMENTO'.");
        }

        CaixaMovimentacao mov = CaixaMovimentacao.builder()
                .caixa(caixa)
                .tipo(tipoEnum)
                .valor(valor)
                .motivo(motivo)
                .dataHora(LocalDateTime.now(ZoneId.of("America/Sao_Paulo")))
                .build();

        return caixaMovimentacaoRepository.save(mov);
    }
}