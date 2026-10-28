package br.edu.unip.aps.meteorologia.service;

import br.edu.unip.aps.meteorologia.dao.ResumoDiarioMeteorologicoDAO;
import br.edu.unip.aps.meteorologia.dto.ResumoDiarioMeteorologicoDTO;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

/** Consulta dados diários já preparados pelo coletor para gráficos e previsão. */
@Service
public class ConsultaResumoDiarioService {

    public static final int LIMITE_PREVISOES = 16;
    private final ResumoDiarioMeteorologicoDAO dao;

    public ConsultaResumoDiarioService(ResumoDiarioMeteorologicoDAO dao) { this.dao = dao; }

    public List<ResumoDiarioMeteorologicoDTO> buscarHistorico(LocalDate inicio, LocalDate fim) {
        if (inicio == null || fim == null || inicio.isAfter(fim)) {
            throw new IllegalArgumentException("Informe um período diário válido");
        }
        return dao.buscarHistorico(inicio, fim).stream().map(ResumoDiarioMeteorologicoDTO::de).toList();
    }

    public List<ResumoDiarioMeteorologicoDTO> buscarPrevisoes(LocalDate hoje, int dias) {
        if (dias < 1 || dias > LIMITE_PREVISOES) {
            throw new IllegalArgumentException("A quantidade de dias deve estar entre 1 e " + LIMITE_PREVISOES);
        }
        return dao.buscarPrevisoes(hoje, dias).stream().map(ResumoDiarioMeteorologicoDTO::de).toList();
    }
}
