package br.edu.unip.aps.meteorologia.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Locale;

/**
 * Faz a comunicação HTTP com a Forecast API da Open-Meteo (não exige chave de API).
 * Todas as variáveis são pedidas numa única requisição por ciclo de coleta.
 */
@Component
public class ApiMeteorologicaClient {

    static final String VARIAVEIS =
            "temperature_2m,relative_humidity_2m,precipitation,rain,wind_speed_10m,weather_code";

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private final URI uri;
    private final Duration timeout;

    public ApiMeteorologicaClient(ObjectMapper objectMapper,
                                  @Value("${openmeteo.url}") String urlBase,
                                  @Value("${openmeteo.latitude}") double latitude,
                                  @Value("${openmeteo.longitude}") double longitude,
                                  @Value("${openmeteo.timezone}") String timezone,
                                  @Value("${openmeteo.timeout-segundos:10}") int timeoutSegundos) {
        this.objectMapper = objectMapper;
        this.timeout = Duration.ofSeconds(timeoutSegundos);
        this.httpClient = HttpClient.newBuilder().connectTimeout(timeout).build();
        this.uri = URI.create(String.format(Locale.ROOT,
                "%s?latitude=%.4f&longitude=%.4f&current=%s&timezone=%s",
                urlBase, latitude, longitude, VARIAVEIS, URLEncoder.encode(timezone, StandardCharsets.UTF_8)));
    }

    /**
     * Consulta as condições atuais na Open-Meteo.
     *
     * @throws ApiMeteorologicaException em timeout, falha de rede, status HTTP de erro ou JSON inválido
     */
    public RespostaOpenMeteo buscarCondicoesAtuais() {
        HttpRequest requisicao = HttpRequest.newBuilder(uri)
                .timeout(timeout)
                .header("Accept", "application/json")
                .GET()
                .build();
        HttpResponse<String> resposta;
        try {
            resposta = httpClient.send(requisicao, HttpResponse.BodyHandlers.ofString());
        } catch (HttpTimeoutException e) {
            throw new ApiMeteorologicaException("Tempo esgotado ao consultar a Open-Meteo (" + timeout.toSeconds() + "s)", e);
        } catch (IOException e) {
            throw new ApiMeteorologicaException("Falha de rede ao consultar a Open-Meteo: " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ApiMeteorologicaException("Consulta à Open-Meteo interrompida", e);
        }

        int status = resposta.statusCode();
        if (status == 429) {
            throw new ApiMeteorologicaException("Limite de requisições da Open-Meteo atingido (HTTP 429)");
        }
        if (status < 200 || status >= 300) {
            throw new ApiMeteorologicaException("Open-Meteo respondeu com HTTP " + status + ": " + resposta.body());
        }
        return converter(resposta.body());
    }

    /** Converte o corpo JSON da resposta, validando que o bloco {@code current} está presente. */
    RespostaOpenMeteo converter(String json) {
        RespostaOpenMeteo resposta;
        try {
            resposta = objectMapper.readValue(json, RespostaOpenMeteo.class);
        } catch (JsonProcessingException e) {
            throw new ApiMeteorologicaException("Resposta da Open-Meteo não é um JSON válido", e);
        }
        if (resposta == null || resposta.current() == null) {
            throw new ApiMeteorologicaException("Resposta da Open-Meteo sem o bloco 'current'");
        }
        return resposta;
    }

    URI getUri() {
        return uri;
    }
}
