package br.com.aps.meteorologia.controller;

import br.com.aps.meteorologia.dao.IEstacaoDAO;
import br.com.aps.meteorologia.model.Estacao;
import br.com.aps.meteorologia.service.GeocodificacaoOpenMeteoService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

/** Seleciona a estação ativa por cidade/UF brasileira e persiste as coordenadas resolvidas. */
@RestController
@RequestMapping("/api/estacao")
public class EstacaoController {
    private final IEstacaoDAO estacoes;
    private final GeocodificacaoOpenMeteoService geocodificacao;

    public EstacaoController(IEstacaoDAO estacoes, GeocodificacaoOpenMeteoService geocodificacao) {
        this.estacoes = estacoes;
        this.geocodificacao = geocodificacao;
    }

    @GetMapping
    public Estacao ativa() {
        return estacoes.buscarAtiva().orElseThrow(() -> new IllegalArgumentException("Nenhuma estação ativa"));
    }

    @PostMapping
    public Estacao selecionar(@RequestBody SolicitarEstacao pedido) {
        if (pedido == null) throw new IllegalArgumentException("Informe cidade e UF");
        var local = geocodificacao.localizarBrasil(pedido.cidade(), pedido.estado());
        return estacoes.salvarEAtivar(local.cidade(), local.estado(), local.latitude(), local.longitude(), local.fusoHorario());
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(IllegalArgumentException.class)
    public java.util.Map<String, String> erro(IllegalArgumentException e) {
        return java.util.Map.of("erro", e.getMessage());
    }

    public record SolicitarEstacao(String cidade, String estado) {}
}
