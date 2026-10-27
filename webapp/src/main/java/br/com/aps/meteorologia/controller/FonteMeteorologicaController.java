package br.com.aps.meteorologia.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;

/** Informa, sem mascarar, a localização e a consulta Open-Meteo usadas pela estação ativa. */
@RestController
public class FonteMeteorologicaController {
    private final String url; private final double latitude; private final double longitude; private final String timezone; private final String cidade;

    public FonteMeteorologicaController(@Value("${openmeteo.url}") String url, @Value("${openmeteo.latitude}") double latitude,
                                        @Value("${openmeteo.longitude}") double longitude, @Value("${openmeteo.timezone}") String timezone,
                                        @Value("${estacao.cidade}") String cidade) {
        this.url = url; this.latitude = latitude; this.longitude = longitude; this.timezone = timezone; this.cidade = cidade;
    }

    @GetMapping("/api/fonte")
    public Map<String, Object> atual() {
        String urlConsulta = UriComponentsBuilder.fromUriString(url).queryParam("latitude", latitude).queryParam("longitude", longitude)
                .queryParam("current", "temperature_2m,relative_humidity_2m,precipitation,rain,wind_speed_10m,weather_code")
                .queryParam("timezone", timezone).build().encode().toUriString();
        return Map.of("nome", "Open-Meteo", "cidade", cidade, "latitude", latitude, "longitude", longitude,
                "timezone", timezone, "urlConsulta", urlConsulta);
    }
}
