package br.com.aps.meteorologia.controller;

import br.com.aps.meteorologia.app.WebappApplication;
import br.com.aps.meteorologia.dao.ILeituraMeteorologicaDAO;
import br.com.aps.meteorologia.dao.IEstacaoDAO;
import br.com.aps.meteorologia.dao.PersistenciaException;
import br.com.aps.meteorologia.model.LeituraMeteorologica;
import br.com.aps.meteorologia.model.ModoOperacao;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

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
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Sobe o webapp (Spring Boot + Tomcat embutido) numa porta livre, com o DAO simulado — sem banco. */
@SpringBootTest(classes = WebappApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class LeituraMeteorologicaServletTest {

    private static final HttpClient HTTP = HttpClient.newHttpClient();

    @LocalServerPort
    private int porta;

    @MockitoBean
    private ILeituraMeteorologicaDAO dao;

    /** Isola os contratos HTTP das regras da estação ativa, cobertas no serviço. */
    @MockitoBean
    private IEstacaoDAO estacaoDAO;

    private HttpResponse<String> get(String caminho) throws IOException, InterruptedException {
        URI uri = URI.create("http://localhost:" + porta + caminho);
        return HTTP.send(HttpRequest.newBuilder(uri).build(), HttpResponse.BodyHandlers.ofString());
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
        l.setLatitude(-23.514938);
        l.setLongitude(-46.610504);
        l.setModoOperacao(ModoOperacao.REAL);
        return l;
    }

    @Test
    void ultimaLeitura() throws Exception {
        when(dao.buscarUltima(null)).thenReturn(Optional.of(leitura(7, 14.0)));

        HttpResponse<String> resposta = get("/api/leituras/ultima");

        assertThat(resposta.statusCode()).isEqualTo(200);
        assertThat(resposta.headers().firstValue("Content-Type")).get().asString().contains("application/json");
        assertThat(resposta.body()).contains("\"id\":7", "\"temperatura\":14.0", "\"descricaoClima\":\"Nublado\"",
                "\"dataHoraColeta\":\"2026-10-20T22:20:05\"", "\"precipitacao\":null",
                "\"latitude\":-23.514938", "\"longitude\":-46.610504", "\"modoOperacao\":\"REAL\"");
    }

    @Test
    void bancoVazioDevolveMensagem() throws Exception {
        when(dao.buscarUltima(null)).thenReturn(Optional.empty());

        HttpResponse<String> resposta = get("/api/leituras/ultima");

        assertThat(resposta.statusCode()).isEqualTo(404);
        assertThat(resposta.body()).contains("Nenhuma leitura registrada ainda");
    }

    @Test
    void ultimasLeituras() throws Exception {
        when(dao.buscarUltimas(2, null)).thenReturn(List.of(leitura(1, 13.0), leitura(2, 14.0)));

        HttpResponse<String> resposta = get("/api/leituras?limite=2");

        assertThat(resposta.statusCode()).isEqualTo(200);
        assertThat(resposta.body()).startsWith("[").contains("\"id\":1", "\"id\":2");
    }

    @Test
    void limiteInvalido() throws Exception {
        assertThat(get("/api/leituras?limite=0").statusCode()).isEqualTo(400);
        assertThat(get("/api/leituras?limite=abc").statusCode()).isEqualTo(400);
        assertThat(get("/api/leituras?limite=1001").statusCode()).isEqualTo(400);
    }

    @Test
    void consultaPorPeriodo() throws Exception {
        when(dao.buscarPorPeriodo(any(), any(), isNull(), eq(1000))).thenReturn(List.of(leitura(1, 13.0)));

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
        when(dao.buscarUltimas(100, null)).thenThrow(new PersistenciaException("Connection refused", null));

        HttpResponse<String> resposta = get("/api/leituras");

        assertThat(resposta.statusCode()).isEqualTo(503);
        assertThat(resposta.body()).contains("Banco de dados indisponível no momento");
    }

    @Test
    void filtraPorModo() throws Exception {
        when(dao.buscarUltimas(eq(100), any())).thenReturn(List.of(leitura(1, 13.0)));
        when(dao.buscarUltima(any())).thenReturn(Optional.of(leitura(1, 13.0)));
        when(dao.buscarPorPeriodo(any(), any(), any(), anyInt())).thenReturn(List.of());

        assertThat(get("/api/leituras?modo=SIMULADO").statusCode()).isEqualTo(200);
        assertThat(get("/api/leituras/ultima?modo=real").statusCode()).isEqualTo(200);
        assertThat(get("/api/leituras/periodo?inicio=2026-10-20T00:00:00&fim=2026-10-20T23:59:59&modo=SIMULADO&limite=50")
                .statusCode()).isEqualTo(200);

        verify(dao).buscarUltimas(100, ModoOperacao.SIMULADO);
        verify(dao).buscarUltima(ModoOperacao.REAL);
        verify(dao).buscarPorPeriodo(LocalDateTime.of(2026, 10, 20, 0, 0), LocalDateTime.of(2026, 10, 20, 23, 59, 59),
                ModoOperacao.SIMULADO, 50);
    }

    @Test
    void modoInvalido() throws Exception {
        HttpResponse<String> resposta = get("/api/leituras?modo=TESTE");

        assertThat(resposta.statusCode()).isEqualTo(400);
        assertThat(resposta.body()).contains("Modo inválido");
    }

    @Test
    void periodoMuitoLongo() throws Exception {
        HttpResponse<String> resposta = get("/api/leituras/periodo?inicio=2026-09-01T00:00:00&fim=2026-10-20T00:00:00");

        assertThat(resposta.statusCode()).isEqualTo(400);
        assertThat(resposta.body()).contains("31 dias");
    }

    @Test
    void totalDeLeituras() throws Exception {
        when(dao.contarTodas()).thenReturn(42L);

        assertThat(get("/api/leituras/total").body()).isEqualTo("{\"total\":42}");
    }

    @Test
    void entregaAPaginaECssEJs() throws Exception {
        HttpResponse<String> pagina = get("/");

        assertThat(pagina.statusCode()).isEqualTo(200);
        assertThat(pagina.body()).contains("Estação Meteorológica");
        assertThat(get("/css/style.css").statusCode()).isEqualTo(200);
        assertThat(get("/js/dashboard.js").statusCode()).isEqualTo(200);
        assertThat(get("/js/grafico.js").statusCode()).isEqualTo(200);
    }
}
