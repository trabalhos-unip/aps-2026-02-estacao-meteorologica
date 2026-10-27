package br.com.aps.meteorologia.controller;

import br.com.aps.meteorologia.dao.PersistenciaException;
import br.com.aps.meteorologia.model.ModoOperacao;
import br.com.aps.meteorologia.service.ConsultaMeteorologicaService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.Map;

/**
 * Servlet que atende {@code /api/leituras/*} e devolve as leituras em JSON para a página Web.
 * Recebe a requisição, chama o serviço e devolve os dados — sem SQL nem Hibernate aqui.
 *
 * <ul>
 *   <li>{@code GET /api/leituras?limite=100&modo=REAL} — últimas leituras em ordem cronológica</li>
 *   <li>{@code GET /api/leituras/ultima?modo=REAL} — leitura mais recente (404 se não houver)</li>
 *   <li>{@code GET /api/leituras/periodo?inicio=2026-10-20T00:00:00&fim=2026-10-20T23:59:59&modo=SIMULADO&limite=1000}
 *       — leituras do período (máx. 31 dias e 1000 registros)</li>
 *   <li>{@code GET /api/leituras/total} — quantidade de leituras</li>
 * </ul>
 * O parâmetro {@code modo} é opcional (REAL ou SIMULADO); sem ele, vêm leituras dos dois modos.
 * Erros também são devolvidos em JSON: {@code {"status": 400, "erro": "mensagem", "dataHora": "..."}}.
 */
public class LeituraMeteorologicaServlet extends HttpServlet {

    private static final Logger LOG = LoggerFactory.getLogger(LeituraMeteorologicaServlet.class);

    private final transient ConsultaMeteorologicaService consultaMeteorologicaService;
    private final transient ObjectMapper conversorJson;

    public LeituraMeteorologicaServlet(ConsultaMeteorologicaService consultaMeteorologicaService,
                                       ObjectMapper conversorJson) {
        this.consultaMeteorologicaService = consultaMeteorologicaService;
        this.conversorJson = conversorJson;
    }

    /** Corpo JSON das respostas de erro, para a página exibir uma mensagem adequada. */
    public record ErroResposta(int status, String erro, LocalDateTime dataHora) {
    }

    @Override
    protected void doGet(HttpServletRequest requisicao, HttpServletResponse resposta) throws IOException {
        String rota = requisicao.getPathInfo() == null ? "/" : requisicao.getPathInfo();
        try {
            switch (rota) {
                case "/" -> enviarRespostaJson(resposta, 200,
                        consultaMeteorologicaService.buscarUltimas(
                                obterParametroInteiro(requisicao, "limite", 100),
                                obterParametroModoOperacao(requisicao)));
                case "/ultima" -> {
                    var ultima = consultaMeteorologicaService.buscarUltima(
                            obterParametroModoOperacao(requisicao));
                    if (ultima.isPresent()) {
                        enviarRespostaJson(resposta, 200, ultima.get());
                    } else {
                        enviarRespostaErro(resposta, 404, "Nenhuma leitura registrada ainda");
                    }
                }
                case "/periodo" -> enviarRespostaJson(resposta, 200,
                        consultaMeteorologicaService.buscarPorPeriodo(
                                obterParametroDataHora(requisicao, "inicio"),
                                obterParametroDataHora(requisicao, "fim"),
                                obterParametroModoOperacao(requisicao),
                                obterParametroInteiro(
                                        requisicao, "limite", ConsultaMeteorologicaService.LIMITE_MAXIMO)));
                case "/total" -> enviarRespostaJson(resposta, 200,
                        Map.of("total", consultaMeteorologicaService.contarTodas()));
                default -> enviarRespostaErro(resposta, 404,
                        "Endereço não encontrado: /api/leituras" + rota);
            }
        } catch (IllegalArgumentException e) {
            enviarRespostaErro(resposta, 400, e.getMessage());
        } catch (PersistenciaException e) {
            LOG.error("Falha ao consultar o banco de dados: {}", e.getMessage());
            enviarRespostaErro(resposta, 503, "Banco de dados indisponível no momento");
        } catch (RuntimeException e) {
            LOG.error("Erro inesperado em {}", requisicao.getRequestURI(), e);
            enviarRespostaErro(resposta, 500, "Erro interno no servidor");
        }
    }

    private int obterParametroInteiro(HttpServletRequest requisicao, String nome, int valorPadrao) {
        String valor = requisicao.getParameter(nome);
        if (valor == null || valor.isBlank()) {
            return valorPadrao;
        }
        try {
            return Integer.parseInt(valor.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Valor inválido para o parâmetro '" + nome + "': " + valor);
        }
    }

    /** Parâmetro opcional "modo": REAL, SIMULADO ou ausente (todos). Valor inválido gera HTTP 400. */
    private ModoOperacao obterParametroModoOperacao(HttpServletRequest requisicao) {
        return ModoOperacao.deTexto(requisicao.getParameter("modo"));
    }

    private LocalDateTime obterParametroDataHora(HttpServletRequest requisicao, String nome) {
        String valor = requisicao.getParameter(nome);
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException("Parâmetro obrigatório ausente: " + nome);
        }
        try {
            return LocalDateTime.parse(valor.trim());
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Valor inválido para o parâmetro '" + nome
                    + "' (use o formato 2026-10-20T14:30:00)");
        }
    }

    private void enviarRespostaErro(HttpServletResponse resposta, int status, String mensagem) throws IOException {
        enviarRespostaJson(resposta, status, new ErroResposta(status, mensagem, LocalDateTime.now()));
    }

    private void enviarRespostaJson(HttpServletResponse resposta, int status, Object corpo) throws IOException {
        resposta.setStatus(status);
        resposta.setContentType("application/json");
        resposta.setCharacterEncoding("UTF-8");
        resposta.setHeader("Cache-Control", "no-store");
        conversorJson.writeValue(resposta.getOutputStream(), corpo);
    }
}
