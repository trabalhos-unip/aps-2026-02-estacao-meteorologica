package br.edu.unip.aps.meteorologia.service;

import br.edu.unip.aps.meteorologia.dao.ResumoHorarioMeteorologicoDAO;
import br.edu.unip.aps.meteorologia.dto.ResumoHorarioMeteorologicoDTO;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class ConsultaResumoHorarioService {
    private static final Duration PERIODO_MAXIMO = Duration.ofDays(31);
    private final ResumoHorarioMeteorologicoDAO dao;

    public ConsultaResumoHorarioService(ResumoHorarioMeteorologicoDAO dao) { this.dao = dao; }

    public List<ResumoHorarioMeteorologicoDTO> buscarHistorico(LocalDateTime inicio, LocalDateTime fim) {
        if (inicio == null || fim == null || inicio.isAfter(fim) || Duration.between(inicio, fim).compareTo(PERIODO_MAXIMO) > 0) {
            throw new IllegalArgumentException("Informe um período horário válido de até 31 dias");
        }
        return dao.buscarHistorico(inicio, fim).stream().map(ResumoHorarioMeteorologicoDTO::de).toList();
    }
}
