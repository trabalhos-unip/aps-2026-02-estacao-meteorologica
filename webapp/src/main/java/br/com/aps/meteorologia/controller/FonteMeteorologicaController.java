package br.com.aps.meteorologia.controller;

import br.com.aps.meteorologia.dao.IEstacaoDAO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;

/** Informa, sem mascarar, a localização e a consulta Open-Meteo usadas pela estação ativa. */
@RestController
public class FonteMeteorologicaController {
    private final String url; private final IEstacaoDAO estacoes;

    public FonteMeteorologicaController(@org.springframework.beans.factory.annotation.Value("${openmeteo.url}") String url, IEstacaoDAO estacoes) {
        this.url = url; this.estacoes = estacoes;
    }

    @GetMapping("/api/fonte")
    public Map<String, Object> atual() {
        var estacao = estacoes.buscarAtiva().orElseThrow(() -> new IllegalArgumentException("Nenhuma estação ativa"));
        double latitude = estacao.getLatitude(), longitude = estacao.getLongitude(); String timezone = estacao.getFusoHorario();
        String urlConsulta = UriComponentsBuilder.fromUriString(url).queryParam("latitude", latitude).queryParam("longitude", longitude)
                .queryParam("current", "temperature_2m,relative_humidity_2m,precipitation,rain,wind_speed_10m,weather_code")
                .queryParam("timezone", timezone).build().encode().toUriString();
        return Map.of("nome", "Open-Meteo", "cidade", estacao.getCidade(), "estado", estacao.getEstado(), "latitude", latitude, "longitude", longitude,
                "timezone", timezone, "urlConsulta", urlConsulta);
    }
}
