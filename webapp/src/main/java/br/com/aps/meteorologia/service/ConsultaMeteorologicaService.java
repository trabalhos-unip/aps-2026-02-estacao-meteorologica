package br.com.aps.meteorologia.service;

import br.com.aps.meteorologia.dao.ILeituraMeteorologicaDAO;
import br.com.aps.meteorologia.dao.IEstacaoDAO;
import br.com.aps.meteorologia.model.LeituraMeteorologica;
import br.com.aps.meteorologia.model.ModoOperacao;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Caso de uso da consulta: valida os parâmetros e busca as leituras pelo DAO.
 * Em todos os métodos, {@code modoOperacao == null} significa "todos os modos" (REAL e SIMULADO).
 */
@Service
public class ConsultaMeteorologicaService {

    /** Limite de registros por consulta, para a página não pedir o banco inteiro. */
    public static final int LIMITE_MAXIMO = 1000;
    /** Período máximo de uma consulta por intervalo de datas. */
    public static final Duration PERIODO_MAXIMO = Duration.ofDays(31);

    private final ILeituraMeteorologicaDAO leituraMeteorologicaDAO;
    private final IEstacaoDAO estacaoDAO;

    public ConsultaMeteorologicaService(ILeituraMeteorologicaDAO leituraMeteorologicaDAO, IEstacaoDAO estacaoDAO) {
        this.leituraMeteorologicaDAO = leituraMeteorologicaDAO;
        this.estacaoDAO = estacaoDAO;
    }

    public Optional<LeituraMeteorologica> buscarUltima(ModoOperacao modoOperacao) {
        Long estacaoId = estacaoAtiva();
        if (estacaoId != null) return filtrar(leituraMeteorologicaDAO.buscarUltimas(LIMITE_MAXIMO, modoOperacao), estacaoId).stream().reduce((a, b) -> b);
        return leituraMeteorologicaDAO.buscarUltima(modoOperacao);
    }

    /** Últimas leituras em ordem cronológica (da mais antiga para a mais recente). */
    public List<LeituraMeteorologica> buscarUltimas(int limite, ModoOperacao modoOperacao) {
        validarLimite(limite);
        Long estacaoId = estacaoAtiva();
        if (estacaoId == null) return leituraMeteorologicaDAO.buscarUltimas(limite, modoOperacao);
        List<LeituraMeteorologica> filtradas = filtrar(leituraMeteorologicaDAO.buscarUltimas(LIMITE_MAXIMO, modoOperacao), estacaoId);
        return filtradas.size() <= limite ? filtradas : filtradas.subList(filtradas.size() - limite, filtradas.size());
    }

    /**
     * Leituras do período em ordem cronológica. O período não pode passar de {@link #PERIODO_MAXIMO}
     * e, se houver mais que {@code limite} leituras, são devolvidas as mais recentes do período.
     */
    public List<LeituraMeteorologica> buscarPorPeriodo(LocalDateTime dataInicio, LocalDateTime dataFim,
                                                       ModoOperacao modoOperacao, int limite) {
        if (dataInicio == null || dataFim == null) {
            throw new IllegalArgumentException("Data de início e data de fim são obrigatórias");
        }
        if (dataInicio.isAfter(dataFim)) {
            throw new IllegalArgumentException("A data de início deve ser anterior à data de fim");
        }
        if (Duration.between(dataInicio, dataFim).compareTo(PERIODO_MAXIMO) > 0) {
            throw new IllegalArgumentException("O período consultado não pode passar de " + PERIODO_MAXIMO.toDays() + " dias");
        }
        validarLimite(limite);
        Long estacaoId = estacaoAtiva();
        int limiteConsulta = estacaoId == null ? limite : LIMITE_MAXIMO;
        List<LeituraMeteorologica> leituras = leituraMeteorologicaDAO.buscarPorPeriodo(
                dataInicio, dataFim, modoOperacao, limiteConsulta);
        List<LeituraMeteorologica> filtradas = estacaoId == null ? leituras : filtrar(leituras, estacaoId);
        return filtradas.size() <= limite ? filtradas : filtradas.subList(filtradas.size() - limite, filtradas.size());
    }

    public long contarTodas() {
        return leituraMeteorologicaDAO.contarTodas();
    }

    private static void validarLimite(int limite) {
        if (limite < 1 || limite > LIMITE_MAXIMO) {
            throw new IllegalArgumentException("O limite deve estar entre 1 e " + LIMITE_MAXIMO);
        }
    }

    private Long estacaoAtiva() {
        try { return estacaoDAO.buscarAtiva().map(e -> e.getId()).orElse(null); }
        catch (RuntimeException ignorada) { return null; }
    }

    private static List<LeituraMeteorologica> filtrar(List<LeituraMeteorologica> leituras, Long estacaoId) {
        return leituras.stream().filter(l -> estacaoId.equals(l.getEstacaoId())).toList();
    }
}
