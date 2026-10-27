package br.com.aps.meteorologia.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/** Série horária da Forecast API, usada para reconstruir histórico e cadastrar previsão diária. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record RespostaSerieOpenMeteo(String timezone, DadosHorarios hourly) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record DadosHorarios(
            List<String> time,
            @JsonProperty("temperature_2m") List<Double> temperatura,
            @JsonProperty("relative_humidity_2m") List<Integer> umidadeRelativa,
            List<Double> precipitation,
            List<Double> rain,
            @JsonProperty("wind_speed_10m") List<Double> velocidadeVento,
            @JsonProperty("weather_code") List<Integer> codigoClima) {
    }
}
