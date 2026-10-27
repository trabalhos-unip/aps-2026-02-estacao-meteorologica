package br.edu.unip.aps.meteorologia.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;

/** Expõe o link da consulta oficial para comparação com as leituras armazenadas. */
@RestController
@RequestMapping("/api/fonte")
public class FonteMeteorologicaController {

    private static final String VARIAVEIS =
            "temperature_2m,relative_humidity_2m,precipitation,rain,wind_speed_10m,weather_code";

    private final String urlBase;
    private final double latitude;
    private final double longitude;
    private final String timezone;
    private final String cidade;

    public FonteMeteorologicaController(
            @Value("${openmeteo.url}") String urlBase,
            @Value("${openmeteo.latitude}") double latitude,
            @Value("${openmeteo.longitude}") double longitude,
            @Value("${openmeteo.timezone}") String timezone,
            @Value("${estacao.cidade}") String cidade) {
        this.urlBase = urlBase;
        this.latitude = latitude;
        this.longitude = longitude;
        this.timezone = timezone;
        this.cidade = cidade;
    }

    /** GET /api/fonte -> URL oficial e coordenadas usadas pelo coletor. */
    @GetMapping
    public Map<String, Object> atual() {
        String urlConsulta = UriComponentsBuilder.fromUriString(urlBase)
                .queryParam("latitude", latitude)
                .queryParam("longitude", longitude)
                .queryParam("current", VARIAVEIS)
                .queryParam("timezone", timezone)
                .build()
                .encode()
                .toUriString();
        return Map.of(
                "nome", "Open-Meteo",
                "latitude", latitude,
                "longitude", longitude,
                "timezone", timezone,
                "cidade", cidade,
                "urlConsulta", urlConsulta);
    }
}
