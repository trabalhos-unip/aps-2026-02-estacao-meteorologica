package br.com.aps.meteorologia.controller;

import br.com.aps.meteorologia.dao.IEstacaoDAO;
import br.com.aps.meteorologia.dao.ResumoDiarioMeteorologicoDAO;
import br.com.aps.meteorologia.dao.ResumoHorarioMeteorologicoDAO;
import br.com.aps.meteorologia.dto.ResumoHorarioMeteorologicoDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

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
    public List<ResumoHorarioMeteorologicoDTO> historicoHorario(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime inicio,
                                                                 @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fim) {
        return horarios.buscarHistorico(estacaoId(), inicio, fim).stream().map(ResumoHorarioMeteorologicoDTO::de).toList();
    }

    @GetMapping("/api/resumos-horarios/previsoes")
    public List<ResumoHorarioMeteorologicoDTO> previsoesHorarias(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime inicio,
                                                                   @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fim) {
        return horarios.buscarPrevisoes(estacaoId(), inicio, fim).stream().map(ResumoHorarioMeteorologicoDTO::de).toList();
    }

    private Long estacaoId() { return estacoes.buscarAtiva().orElseThrow(() -> new IllegalArgumentException("Nenhuma estação ativa")).getId(); }

    /** Parâmetro fora do intervalo ou estação ausente: 400 com a mensagem, em vez de 500. */
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(IllegalArgumentException.class)
    public Map<String, String> erro(IllegalArgumentException e) {
        return Map.of("erro", e.getMessage());
    }
}
