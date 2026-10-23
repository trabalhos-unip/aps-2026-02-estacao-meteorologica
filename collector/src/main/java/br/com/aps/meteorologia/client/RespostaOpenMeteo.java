package br.com.aps.meteorologia.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Parte da resposta JSON da Forecast API da Open-Meteo que o projeto utiliza.
 * Campos não listados aqui são ignorados.
 *
 * <pre>
 * {"latitude":-23.51, "longitude":-46.61, "timezone":"America/Sao_Paulo",
 *  "current":{"time":"2026-10-20T22:15", "interval":900, "temperature_2m":14.0,
 *             "relative_humidity_2m":89, "precipitation":0.0, "rain":0.0,
 *             "wind_speed_10m":9.8, "weather_code":3}}
 * </pre>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record RespostaOpenMeteo(Double latitude, Double longitude, String timezone, CondicoesAtuais current) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record CondicoesAtuais(
            String time,
            Integer interval,
            @JsonProperty("temperature_2m") Double temperatura,
            @JsonProperty("relative_humidity_2m") Integer umidadeRelativa,
            @JsonProperty("precipitation") Double precipitacao,
            @JsonProperty("rain") Double chuva,
            @JsonProperty("wind_speed_10m") Double velocidadeVento,
            @JsonProperty("weather_code") Integer codigoClima) {
    }
}
