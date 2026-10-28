package br.edu.unip.aps.meteorologia.controller;

import br.edu.unip.aps.meteorologia.dto.ResumoHorarioMeteorologicoDTO;
import br.edu.unip.aps.meteorologia.service.ConsultaResumoHorarioService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

/** Pontos horários históricos, usados quando a estação não coletou durante parte do dia. */
@RestController
@RequestMapping("/api/resumos-horarios")
public class ResumoHorarioMeteorologicoController {
    private final ConsultaResumoHorarioService service;
    public ResumoHorarioMeteorologicoController(ConsultaResumoHorarioService service) { this.service = service; }

    @GetMapping("/historico")
    public List<ResumoHorarioMeteorologicoDTO> historico(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime inicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fim) {
        return service.buscarHistorico(inicio, fim);
    }
}
