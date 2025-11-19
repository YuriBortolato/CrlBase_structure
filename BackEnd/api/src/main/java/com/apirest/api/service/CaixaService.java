package com.apirest.api.service;

import com.apirest.api.dto.*;
import com.apirest.api.entity.*;
import com.apirest.api.repository.CaixaRepository;
import com.apirest.api.repository.FuncionarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class CaixaService {

    private final CaixaRepository caixaRepository;
    private final FuncionarioRepository funcionarioRepository;

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

    //  Fechamento de Caixa
    @Transactional
    public Caixa fecharCaixa(Long idCaixa, CaixaFechamentoDTO dto) {
        log.info("Tentativa de fechamento de caixa ID: {}", idCaixa);

        Caixa caixa = caixaRepository.findById(idCaixa)
                .orElseThrow(() -> new RuntimeException("Caixa não encontrado"));

        if (caixa.getStatus() == StatusCaixa.FECHADO) {
            throw new RuntimeException("Este caixa já está fechado.");
        }

        //  Calcular total do SISTEMA (Vendas)
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

        // Calcular Quebra
        BigDecimal esperado = caixa.getSaldoInicial().add(totalVendasSistema);
        BigDecimal quebra = totalInformado.subtract(esperado);

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

    // Listagem de Caixas com Filtros
    public List<Caixa> listarComFiltros(Long idFuncionario, StatusCaixa status, LocalDateTime inicio, LocalDateTime fim) {
        return caixaRepository.findByFiltros(idFuncionario, status, inicio, fim);
    }

    // Resumo Dashboard (Para a tela de admin)
    public DashboardResumoDTO calcularResumoDashboard(List<Caixa> caixasFiltrados) {
        return calcularSomaDeCaixas(caixasFiltrados);
    }

    //  Relatório Individual (Para o funcionário ver o dele)
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
                .totalGeralHoje(resumoHoje.getTotalGeral())
                .dinheiroHoje(resumoHoje.getTotalDinheiro())
                .pixHoje(resumoHoje.getTotalPix())
                .debitoHoje(resumoHoje.getTotalDebito())
                .creditoHoje(resumoHoje.getTotalCredito())
                .crediarioHoje(resumoHoje.getTotalCrediario())
                .totalGeralMes(resumoMes.getTotalGeral())
                .dinheiroMes(resumoMes.getTotalDinheiro())
                .pixMes(resumoMes.getTotalPix())
                .debitoMes(resumoMes.getTotalDebito())
                .creditoMes(resumoMes.getTotalCredito())
                .crediarioMes(resumoMes.getTotalCrediario())
                .build();
    }

    // Conversão para DTO de Resposta
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

    //  Relatório Global por Data Específica (Dia X)
    public RelatorioPeriodoDTO gerarRelatorioDoDia(LocalDate data) {
        LocalDateTime inicio = data.atStartOfDay();
        LocalDateTime fim = data.atTime(LocalTime.MAX);

        List<Caixa> caixas = caixaRepository.findAllCaixasFechadosPorPeriodo(inicio, fim);
        DashboardResumoDTO soma = calcularSomaDeCaixas(caixas);

        return RelatorioPeriodoDTO.builder()
                .periodo(data.toString())
                .totalGeral(soma.getTotalGeral())
                .totalDinheiro(soma.getTotalDinheiro())
                .totalPix(soma.getTotalPix())
                .totalDebito(soma.getTotalDebito())
                .totalCredito(soma.getTotalCredito())
                .totalCrediario(soma.getTotalCrediario())
                .quantidadeCaixasFechados(caixas.size())
                .build();
    }

    // Relatório Global por Mês (Mês X)
    public RelatorioPeriodoDTO gerarRelatorioDoMes(int ano, int mes) {
        LocalDate dataInicial = LocalDate.of(ano, mes, 1);
        LocalDateTime inicio = dataInicial.atStartOfDay();
        LocalDateTime fim = dataInicial.withDayOfMonth(dataInicial.lengthOfMonth()).atTime(LocalTime.MAX);

        List<Caixa> caixas = caixaRepository.findAllCaixasFechadosPorPeriodo(inicio, fim);
        DashboardResumoDTO soma = calcularSomaDeCaixas(caixas);

        return RelatorioPeriodoDTO.builder()
                .periodo(mes + "/" + ano)
                .totalGeral(soma.getTotalGeral())
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
        BigDecimal dinheiro = BigDecimal.ZERO;
        BigDecimal pix = BigDecimal.ZERO;
        BigDecimal debito = BigDecimal.ZERO;
        BigDecimal credito = BigDecimal.ZERO;
        BigDecimal crediario = BigDecimal.ZERO;
        BigDecimal quebra = BigDecimal.ZERO;

        for (Caixa c : listaCaixas) {
            if (c.getConferidoDinheiro() != null) dinheiro = dinheiro.add(c.getConferidoDinheiro());
            if (c.getConferidoPix() != null) pix = pix.add(c.getConferidoPix());
            if (c.getConferidoDebito() != null) debito = debito.add(c.getConferidoDebito());
            if (c.getConferidoCredito() != null) credito = credito.add(c.getConferidoCredito());
            if (c.getConferidoCrediario() != null) crediario = crediario.add(c.getConferidoCrediario());
            if (c.getQuebraDeCaixa() != null) quebra = quebra.add(c.getQuebraDeCaixa());
        }

        BigDecimal geral = dinheiro.add(pix).add(debito).add(credito).add(crediario);

        return DashboardResumoDTO.builder()
                .totalDinheiro(dinheiro)
                .totalPix(pix)
                .totalDebito(debito)
                .totalCredito(credito)
                .totalCrediario(crediario)
                .totalGeral(geral)
                .totalQuebra(quebra)
                .build();
    }
}