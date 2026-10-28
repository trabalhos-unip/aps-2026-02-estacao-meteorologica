package br.com.aps.meteorologia.controller;

import br.com.aps.meteorologia.dao.IEstacaoDAO;
import br.com.aps.meteorologia.dao.ResumoDiarioMeteorologicoDAO;
import br.com.aps.meteorologia.dao.ResumoHorarioMeteorologicoDAO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

/** APIs de histórico e previsão da estação configurada; nunca combinam cidades. */
@RestController
public class ResumoMeteorologicoController {
    private final ResumoDiarioMeteorologicoDAO diarios; private final ResumoHorarioMeteorologicoDAO horarios;
    private final IEstacaoDAO estacoes; private final ZoneId zona;

    public ResumoMeteorologicoController(ResumoDiarioMeteorologicoDAO diarios, ResumoHorarioMeteorologicoDAO horarios, IEstacaoDAO estacoes,
            @Value("${openmeteo.timezone}") String timezone) {
        this.diarios = diarios; this.horarios = horarios; this.estacoes = estacoes; this.zona = ZoneId.of(timezone);
    }

    @GetMapping("/api/resumos-diarios/historico")
    public List<?> historicoDiario(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
                                   @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim) {
        return diarios.buscarHistorico(estacaoId(), inicio, fim);
    }

    @GetMapping("/api/resumos-diarios/previsoes")
    public List<?> previsoes(@RequestParam(defaultValue = "5") int dias) {
        if (dias < 1 || dias > 16) throw new IllegalArgumentException("dias deve estar entre 1 e 16");
        return diarios.buscarPrevisoes(estacaoId(), LocalDate.now(zona), dias);
    }

    @GetMapping("/api/resumos-horarios/historico")
    public List<?> historicoHorario(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime inicio,
                                    @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fim) {
        return horarios.buscarHistorico(estacaoId(), inicio, fim);
    }

    private Long estacaoId() { return estacoes.buscarAtiva().orElseThrow(() -> new IllegalArgumentException("Nenhuma estação ativa")).getId(); }
}
