package br.com.aps.meteorologia.client;

import br.com.aps.meteorologia.model.LeituraMeteorologica;
import br.com.aps.meteorologia.model.ModoOperacao;
import br.com.aps.meteorologia.model.TipoErroColeta;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
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

/** Fonte real de dados meteorológicos obtidos pela Forecast API da Open-Meteo. */
@Component
@ConditionalOnProperty(name = "coleta.modo", havingValue = "REAL", matchIfMissing = true)
public class ClienteOpenMeteo implements IFonteMeteorologica {

    static final String VARIAVEIS_METEOROLOGICAS =
            "temperature_2m,relative_humidity_2m,precipitation,rain,wind_speed_10m,weather_code,"
                    + "apparent_temperature,surface_pressure,wind_direction_10m,wind_gusts_10m,cloud_cover";

    private final ObjectMapper conversorJson;
    private final HttpClient clienteHttp;
    private final URI enderecoConsulta;
    private final Duration tempoLimite;

    public ClienteOpenMeteo(ObjectMapper conversorJson,
                            @Value("${openmeteo.url}") String urlBase,
                            @Value("${openmeteo.latitude}") double latitude,
                            @Value("${openmeteo.longitude}") double longitude,
                            @Value("${openmeteo.timezone}") String fusoHorario,
                            @Value("${openmeteo.timeout-segundos:10}") int tempoLimiteSegundos) {
        this.conversorJson = conversorJson;
        this.tempoLimite = Duration.ofSeconds(tempoLimiteSegundos);
        this.clienteHttp = HttpClient.newBuilder().connectTimeout(tempoLimite).build();
        this.enderecoConsulta = criarEnderecoConsulta(urlBase, latitude, longitude, fusoHorario);
    }

    @Override
    public DadosMeteorologicosAtuais obterDadosAtuais() {
        HttpRequest requisicao = HttpRequest.newBuilder(enderecoConsulta)
                .timeout(tempoLimite)
                .header("Accept", "application/json")
                .GET()
                .build();
        HttpResponse<String> resposta = enviarRequisicao(requisicao);
        validarStatusHttp(resposta);
        return normalizarResposta(converterRespostaJson(resposta.body()));
    }

    @Override
    public ModoOperacao obterModoOperacao() {
        return ModoOperacao.REAL;
    }

    @Override
    public String obterNomeFonte() {
        return LeituraMeteorologica.FONTE_OPEN_METEO;
    }

    URI obterEnderecoConsulta() {
        return enderecoConsulta;
    }

    private HttpResponse<String> enviarRequisicao(HttpRequest requisicao) {
        try {
            return clienteHttp.send(requisicao, HttpResponse.BodyHandlers.ofString());
        } catch (HttpTimeoutException erro) {
            throw new FonteMeteorologicaException(
                    "Tempo esgotado ao consultar a Open-Meteo (" + tempoLimite.toSeconds() + "s)",
                    TipoErroColeta.TIMEOUT, erro);
        } catch (IOException erro) {
            throw new FonteMeteorologicaException(
                    "Falha de rede ao consultar a Open-Meteo: " + erro.getMessage(),
                    TipoErroColeta.REDE, erro);
        } catch (InterruptedException erro) {
            Thread.currentThread().interrupt();
            throw new FonteMeteorologicaException(
                    "Consulta à Open-Meteo interrompida", TipoErroColeta.REDE, erro);
        }
    }

    private static void validarStatusHttp(HttpResponse<String> resposta) {
        int codigoHttp = resposta.statusCode();
        if (codigoHttp == 429) {
            throw new FonteMeteorologicaException(
                    "Limite de requisições da Open-Meteo atingido (HTTP 429)",
                    TipoErroColeta.LIMITE_API, codigoHttp);
        }
        if (codigoHttp < 200 || codigoHttp >= 300) {
            throw new FonteMeteorologicaException(
                    "Open-Meteo respondeu com HTTP " + codigoHttp + ": " + resposta.body(),
                    TipoErroColeta.HTTP, codigoHttp);
        }
    }

    private RespostaOpenMeteo converterRespostaJson(String json) {
        try {
            RespostaOpenMeteo resposta = conversorJson.readValue(json, RespostaOpenMeteo.class);
            if (resposta == null || resposta.current() == null) {
                throw new FonteMeteorologicaException(
                        "Resposta da Open-Meteo sem o bloco 'current'", TipoErroColeta.RESPOSTA_INVALIDA);
            }
            return resposta;
        } catch (JsonProcessingException erro) {
            throw new FonteMeteorologicaException(
                    "Resposta da Open-Meteo não é um JSON válido", TipoErroColeta.RESPOSTA_INVALIDA, erro);
        }
    }

    private static DadosMeteorologicosAtuais normalizarResposta(RespostaOpenMeteo resposta) {
        RespostaOpenMeteo.CondicoesAtuais atual = resposta.current();
        return new DadosMeteorologicosAtuais(
                resposta.latitude(), resposta.longitude(), atual.time(), atual.temperatura(),
                atual.umidadeRelativa(), atual.precipitacao(), atual.chuva(), atual.velocidadeVento(),
                atual.codigoClima(), atual.sensacaoTermica(), atual.pressaoSuperficie(),
                atual.direcaoVento(), atual.rajadaVento(), atual.coberturaNuvens());
    }

    private static URI criarEnderecoConsulta(String urlBase, double latitude, double longitude, String fusoHorario) {
        return URI.create(String.format(Locale.ROOT,
                "%s?latitude=%.4f&longitude=%.4f&current=%s&timezone=%s",
                urlBase, latitude, longitude, VARIAVEIS_METEOROLOGICAS,
                URLEncoder.encode(fusoHorario, StandardCharsets.UTF_8)));
    }
}
