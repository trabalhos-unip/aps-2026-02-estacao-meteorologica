package br.edu.unip.aps.meteorologia.controller;

import br.edu.unip.aps.meteorologia.dto.ErroResposta;
import br.edu.unip.aps.meteorologia.dto.LeituraMeteorologicaDTO;
import br.edu.unip.aps.meteorologia.service.ConsultaMeteorologicaService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Endpoints JSON consumidos pela página Web (dashboard e gráfico).
 * Recebe a requisição, chama o serviço e devolve os dados — sem SQL nem Hibernate aqui.
 * (No Spring, este controller é atendido pelo DispatcherServlet, que faz o papel do Servlet.)
 */
@RestController
@RequestMapping("/api/leituras")
public class LeituraMeteorologicaController {

    private final ConsultaMeteorologicaService service;

    public LeituraMeteorologicaController(ConsultaMeteorologicaService service) {
        this.service = service;
    }

    /** GET /api/leituras/ultima → leitura mais recente; 404 se o banco ainda não tem leituras. */
    @GetMapping("/ultima")
    public ResponseEntity<?> ultima() {
        return service.buscarUltima()
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ErroResposta.de(404, "Nenhuma leitura registrada ainda")));
    }

    /** GET /api/leituras?limite=100 → últimas leituras em ordem cronológica (lista vazia se não houver). */
    @GetMapping
    public List<LeituraMeteorologicaDTO> ultimas(@RequestParam(defaultValue = "100") int limite) {
        return service.buscarUltimas(limite);
    }

    /** GET /api/leituras/periodo?inicio=2026-10-20T00:00:00&amp;fim=2026-10-20T23:59:59 → leituras do período. */
    @GetMapping("/periodo")
    public List<LeituraMeteorologicaDTO> periodo(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime inicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fim) {
        return service.buscarPorPeriodo(inicio, fim);
    }

    /** GET /api/leituras/total → quantidade de leituras gravadas. */
    @GetMapping("/total")
    public Map<String, Long> total() {
        return Map.of("total", service.contar());
    }
}
