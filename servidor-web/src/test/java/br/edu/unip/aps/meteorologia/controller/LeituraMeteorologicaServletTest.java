package br.edu.unip.aps.meteorologia.controller;

import br.edu.unip.aps.meteorologia.ServidorWebApplication;
import br.edu.unip.aps.meteorologia.dao.LeituraMeteorologicaDAO;
import br.edu.unip.aps.meteorologia.dao.PersistenciaException;
import br.edu.unip.aps.meteorologia.model.LeituraMeteorologica;
import br.edu.unip.aps.meteorologia.service.ConsultaMeteorologicaService;
import org.apache.catalina.startup.Tomcat;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

/** Sobe o Tomcat embutido de verdade (porta livre) com o DAO simulado — sem banco. */
class LeituraMeteorologicaServletTest {

    private static final LeituraMeteorologicaDAO DAO = mock(LeituraMeteorologicaDAO.class);
    private static final HttpClient HTTP = HttpClient.newHttpClient();
    private static Tomcat tomcat;
    private static String base;

    @BeforeAll
    static void subirServidor() throws Exception {
        tomcat = ServidorWebApplication.criarServidor(0, new ConsultaMeteorologicaService(DAO));
        tomcat.start();
        base = "http://localhost:" + tomcat.getConnector().getLocalPort();
    }

    @AfterAll
    static void pararServidor() throws Exception {
        tomcat.stop();
        tomcat.destroy();
    }

    @BeforeEach
    void limparSimulacao() {
        reset(DAO);
    }

    private HttpResponse<String> get(String caminho) throws IOException, InterruptedException {
        return HTTP.send(HttpRequest.newBuilder(URI.create(base + caminho)).build(), HttpResponse.BodyHandlers.ofString());
    }

    private LeituraMeteorologica leitura(long id, double temperatura) {
        LeituraMeteorologica l = new LeituraMeteorologica();
        l.setId(id);
        l.setDataHoraColeta(LocalDateTime.of(2026, 10, 20, 22, 20, 5));
        l.setDataHoraFonte(LocalDateTime.of(2026, 10, 20, 22, 15));
        l.setTemperatura(temperatura);
        l.setUmidadeRelativa(89);
        l.setCodigoClima(3);
        l.setPresencaPrecipitacao(false);
        return l;
    }

    @Test
    void ultimaLeitura() throws Exception {
        when(DAO.buscarUltima()).thenReturn(Optional.of(leitura(7, 14.0)));

        HttpResponse<String> resposta = get("/api/leituras/ultima");

        assertThat(resposta.statusCode()).isEqualTo(200);
        assertThat(resposta.headers().firstValue("Content-Type")).get().asString().contains("application/json");
        assertThat(resposta.body()).contains("\"id\":7", "\"temperatura\":14.0", "\"descricaoClima\":\"Nublado\"",
                "\"dataHoraColeta\":\"2026-10-20T22:20:05\"", "\"precipitacao\":null");
    }

    @Test
    void bancoVazioDevolveMensagem() throws Exception {
        when(DAO.buscarUltima()).thenReturn(Optional.empty());

        HttpResponse<String> resposta = get("/api/leituras/ultima");

        assertThat(resposta.statusCode()).isEqualTo(404);
        assertThat(resposta.body()).contains("Nenhuma leitura registrada ainda");
    }

    @Test
    void ultimasLeituras() throws Exception {
        when(DAO.buscarUltimas(2)).thenReturn(List.of(leitura(1, 13.0), leitura(2, 14.0)));

        HttpResponse<String> resposta = get("/api/leituras?limite=2");

        assertThat(resposta.statusCode()).isEqualTo(200);
        assertThat(resposta.body()).startsWith("[").contains("\"id\":1", "\"id\":2");
    }

    @Test
    void limiteInvalido() throws Exception {
        assertThat(get("/api/leituras?limite=0").statusCode()).isEqualTo(400);
        assertThat(get("/api/leituras?limite=abc").statusCode()).isEqualTo(400);
    }

    @Test
    void consultaPorPeriodo() throws Exception {
        when(DAO.buscarPorPeriodo(any(), any())).thenReturn(List.of(leitura(1, 13.0)));

        HttpResponse<String> resposta = get("/api/leituras/periodo?inicio=2026-10-20T00:00:00&fim=2026-10-20T23:59:59");

        assertThat(resposta.statusCode()).isEqualTo(200);
        assertThat(resposta.body()).contains("\"id\":1");
    }

    @Test
    void periodoComDataInvalidaOuAusente() throws Exception {
        assertThat(get("/api/leituras/periodo?inicio=ontem&fim=hoje").statusCode()).isEqualTo(400);
        assertThat(get("/api/leituras/periodo").body()).contains("Parâmetro obrigatório ausente: inicio");
    }

    @Test
    void bancoIndisponivel() throws Exception {
        when(DAO.buscarUltimas(100)).thenThrow(new PersistenciaException("Connection refused", null));

        HttpResponse<String> resposta = get("/api/leituras");

        assertThat(resposta.statusCode()).isEqualTo(503);
        assertThat(resposta.body()).contains("Banco de dados indisponível no momento");
    }

    @Test
    void totalDeLeituras() throws Exception {
        when(DAO.contar()).thenReturn(42L);

        assertThat(get("/api/leituras/total").body()).isEqualTo("{\"total\":42}");
    }

    @Test
    void entregaAPaginaInicial() throws Exception {
        HttpResponse<String> resposta = get("/");

        assertThat(resposta.statusCode()).isEqualTo(200);
        assertThat(resposta.body()).contains("Estação Meteorológica");
        assertThat(get("/css/style.css").statusCode()).isEqualTo(200);
    }
}
