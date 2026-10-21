package br.edu.unip.aps.meteorologia.service;

import br.edu.unip.aps.meteorologia.dao.LeituraMeteorologicaDAO;
import br.edu.unip.aps.meteorologia.dto.LeituraMeteorologicaDTO;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/** Caso de uso da consulta: valida os parâmetros, busca pelo DAO e converte para o formato da página. */
@Service
public class ConsultaMeteorologicaService {

    /** Limite de registros por consulta, para a página não pedir o banco inteiro. */
    public static final int LIMITE_MAXIMO = 1000;
    /** Período máximo de uma consulta por intervalo de datas. */
    public static final Duration PERIODO_MAXIMO = Duration.ofDays(31);

    private final LeituraMeteorologicaDAO dao;

    public ConsultaMeteorologicaService(LeituraMeteorologicaDAO dao) {
        this.dao = dao;
    }

    public Optional<LeituraMeteorologicaDTO> buscarUltima() {
        return dao.buscarUltima().map(LeituraMeteorologicaDTO::de);
    }

    /** Últimas leituras em ordem cronológica (da mais antiga para a mais recente). */
    public List<LeituraMeteorologicaDTO> buscarUltimas(int limite) {
        if (limite < 1 || limite > LIMITE_MAXIMO) {
            throw new IllegalArgumentException("O limite deve estar entre 1 e " + LIMITE_MAXIMO);
        }
        return dao.buscarUltimas(limite).stream().map(LeituraMeteorologicaDTO::de).toList();
    }

    public List<LeituraMeteorologicaDTO> buscarPorPeriodo(LocalDateTime inicio, LocalDateTime fim) {
        if (inicio.isAfter(fim)) {
            throw new IllegalArgumentException("A data de início deve ser anterior à data de fim");
        }
        if (Duration.between(inicio, fim).compareTo(PERIODO_MAXIMO) > 0) {
            throw new IllegalArgumentException("O período consultado não pode passar de " + PERIODO_MAXIMO.toDays() + " dias");
        }
        return dao.buscarPorPeriodo(inicio, fim).stream().map(LeituraMeteorologicaDTO::de).toList();
    }

    public long contar() {
        return dao.contar();
    }
}
