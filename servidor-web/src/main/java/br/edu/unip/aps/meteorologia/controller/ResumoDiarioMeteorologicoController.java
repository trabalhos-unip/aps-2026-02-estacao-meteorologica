package br.edu.unip.aps.meteorologia.controller;

import br.edu.unip.aps.meteorologia.dto.ResumoDiarioMeteorologicoDTO;
import br.edu.unip.aps.meteorologia.service.ConsultaResumoDiarioService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

/** Endpoints para a página exibir histórico reconstituído e previsão, sempre separados. */
@RestController
@RequestMapping("/api/resumos-diarios")
public class ResumoDiarioMeteorologicoController {

    private final ConsultaResumoDiarioService service;
    private final ZoneId zona;

    public ResumoDiarioMeteorologicoController(ConsultaResumoDiarioService service,
                                                @org.springframework.beans.factory.annotation.Value("${openmeteo.timezone}") String timezone) {
        this.service = service;
        this.zona = ZoneId.of(timezone);
    }

    /** GET /api/resumos-diarios/historico?inicio=2026-10-01&fim=2026-10-15 */
    @GetMapping("/historico")
    public List<ResumoDiarioMeteorologicoDTO> historico(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim) {
        return service.buscarHistorico(inicio, fim);
    }

    /** GET /api/resumos-diarios/previsoes?dias=5 */
    @GetMapping("/previsoes")
    public List<ResumoDiarioMeteorologicoDTO> previsoes(@RequestParam(defaultValue = "5") int dias) {
        return service.buscarPrevisoes(LocalDate.now(zona), dias);
    }
}
