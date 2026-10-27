package br.com.aps.meteorologia.controller;

import br.com.aps.meteorologia.dao.IEstacaoDAO;
import br.com.aps.meteorologia.model.Estacao;
import br.com.aps.meteorologia.service.GeocodificacaoOpenMeteoService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
        if (pedido.cidade() == null || pedido.cidade().isBlank()) throw new IllegalArgumentException("Informe a cidade");
        if (pedido.estado() == null || !pedido.estado().trim().matches("(?i)[a-z]{2}")) {
            throw new IllegalArgumentException("Informe a sigla do estado, por exemplo SP");
        }
        var existente = estacoes.buscarPorCidadeEEstado(pedido.cidade(), pedido.estado());
        if (existente.isPresent()) {
            var estacao = existente.get();
            return estacoes.salvarEAtivar(estacao.getCidade(), estacao.getEstado(),
                    estacao.getLatitude(), estacao.getLongitude(), estacao.getFusoHorario());
        }
        var local = geocodificacao.localizarBrasil(pedido.cidade(), pedido.estado());
        return estacoes.salvarEAtivar(local.cidade(), local.estado(), local.latitude(), local.longitude(), local.fusoHorario());
    }

    @GetMapping("/sugestoes")
    public List<GeocodificacaoOpenMeteoService.Sugestao> sugestoes(
            @RequestParam String termo, @RequestParam(required = false) String estado) {
        return geocodificacao.buscarSugestoesBrasil(termo, estado);
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(IllegalArgumentException.class)
    public java.util.Map<String, String> erro(IllegalArgumentException e) {
        return java.util.Map.of("erro", e.getMessage());
    }

    public record SolicitarEstacao(String cidade, String estado) {}
}
