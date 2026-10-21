package br.edu.unip.aps.meteorologia.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Testa o cliente contra um servidor HTTP local que imita a Open-Meteo (sem acessar a internet). */
class ApiMeteorologicaClientTest {

    /** Resposta real obtida da Open-Meteo em 20/10/2026. */
    static final String JSON_REAL = """
            {"latitude":-23.514938,"longitude":-46.610504,"generationtime_ms":0.14,"utc_offset_seconds":-10800,
             "timezone":"America/Sao_Paulo","timezone_abbreviation":"GMT-3","elevation":737.0,
             "current_units":{"time":"iso8601","interval":"seconds","temperature_2m":"°C","relative_humidity_2m":"%",
                              "precipitation":"mm","rain":"mm","wind_speed_10m":"km/h","weather_code":"wmo code"},
             "current":{"time":"2026-10-20T22:15","interval":900,"temperature_2m":14.0,"relative_humidity_2m":89,
                        "precipitation":0.00,"rain":0.00,"wind_speed_10m":9.8,"weather_code":3}}
            """;

    private HttpServer servidor;

    private ApiMeteorologicaClient clienteRespondendo(int status, String corpo) throws IOException {
        servidor = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        servidor.createContext("/v1/forecast", troca -> {
            byte[] bytes = corpo.getBytes(StandardCharsets.UTF_8);
            troca.sendResponseHeaders(status, bytes.length);
            try (OutputStream saida = troca.getResponseBody()) {
                saida.write(bytes);
            }
        });
        servidor.start();
        String url = "http://127.0.0.1:" + servidor.getAddress().getPort() + "/v1/forecast";
        return new ApiMeteorologicaClient(new ObjectMapper(), url, -23.5505, -46.6333, "America/Sao_Paulo", 5);
    }

    @AfterEach
    void pararServidor() {
        if (servidor != null) {
            servidor.stop(0);
        }
    }

    @Test
    void converteRespostaReal() throws IOException {
        RespostaOpenMeteo resposta = clienteRespondendo(200, JSON_REAL).buscarCondicoesAtuais();

        RespostaOpenMeteo.CondicoesAtuais atuais = resposta.current();
        assertThat(atuais.time()).isEqualTo("2026-10-20T22:15");
        assertThat(atuais.interval()).isEqualTo(900);
        assertThat(atuais.temperatura()).isEqualTo(14.0);
        assertThat(atuais.umidadeRelativa()).isEqualTo(89);
        assertThat(atuais.precipitacao()).isEqualTo(0.0);
        assertThat(atuais.chuva()).isEqualTo(0.0);
        assertThat(atuais.velocidadeVento()).isEqualTo(9.8);
        assertThat(atuais.codigoClima()).isEqualTo(3);
    }

    @Test
    void montaUrlComTodasAsVariaveis() throws IOException {
        String uri = clienteRespondendo(200, JSON_REAL).getUri().toString();

        assertThat(uri).contains("latitude=-23.5505", "longitude=-46.6333",
                "current=" + ApiMeteorologicaClient.VARIAVEIS, "timezone=America%2FSao_Paulo");
    }

    @Test
    void limiteDeRequisicoes() throws IOException {
        ApiMeteorologicaClient cliente = clienteRespondendo(429, "{\"error\":true}");

        assertThatThrownBy(cliente::buscarCondicoesAtuais)
                .isInstanceOf(ApiMeteorologicaException.class).hasMessageContaining("429");
    }

    @Test
    void apiIndisponivel() throws IOException {
        ApiMeteorologicaClient cliente = clienteRespondendo(503, "Service Unavailable");

        assertThatThrownBy(cliente::buscarCondicoesAtuais)
                .isInstanceOf(ApiMeteorologicaException.class).hasMessageContaining("503");
    }

    @Test
    void respostaInvalida() throws IOException {
        ApiMeteorologicaClient cliente = clienteRespondendo(200, "<html>isto não é JSON</html>");

        assertThatThrownBy(cliente::buscarCondicoesAtuais)
                .isInstanceOf(ApiMeteorologicaException.class).hasMessageContaining("JSON");
    }

    @Test
    void respostaSemBlocoCurrent() throws IOException {
        ApiMeteorologicaClient cliente = clienteRespondendo(200, "{\"latitude\":-23.5}");

        assertThatThrownBy(cliente::buscarCondicoesAtuais)
                .isInstanceOf(ApiMeteorologicaException.class).hasMessageContaining("current");
    }

    @Test
    void semConexao() {
        // Porta 1 local: ninguém escutando → falha de conexão
        ApiMeteorologicaClient cliente = new ApiMeteorologicaClient(new ObjectMapper(),
                "http://127.0.0.1:1/v1/forecast", 0, 0, "America/Sao_Paulo", 2);

        assertThatThrownBy(cliente::buscarCondicoesAtuais).isInstanceOf(ApiMeteorologicaException.class);
    }
}
