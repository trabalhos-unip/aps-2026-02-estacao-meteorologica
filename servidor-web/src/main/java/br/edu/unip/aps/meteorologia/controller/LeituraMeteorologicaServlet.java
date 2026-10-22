package br.edu.unip.aps.meteorologia.controller;

import br.edu.unip.aps.meteorologia.dao.PersistenciaException;
import br.edu.unip.aps.meteorologia.dto.ErroResposta;
import br.edu.unip.aps.meteorologia.service.ConsultaMeteorologicaService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Servlet que atende {@code /api/leituras/*} e devolve as leituras em JSON para a página Web.
 * Recebe a requisição, chama o serviço e devolve os dados — sem SQL nem Hibernate aqui.
 *
 * <ul>
 *   <li>{@code GET /api/leituras?limite=100} — últimas leituras em ordem cronológica</li>
 *   <li>{@code GET /api/leituras/ultima} — leitura mais recente (404 se o banco estiver vazio)</li>
 *   <li>{@code GET /api/leituras/periodo?inicio=2026-10-20T00:00:00&fim=2026-10-20T23:59:59}</li>
 *   <li>{@code GET /api/leituras/total} — quantidade de leituras</li>
 * </ul>
 */
public class LeituraMeteorologicaServlet extends HttpServlet {

    private static final Logger log = Logger.getLogger(LeituraMeteorologicaServlet.class.getName());

    private final transient ConsultaMeteorologicaService service;
    private final transient ObjectMapper json = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    public LeituraMeteorologicaServlet(ConsultaMeteorologicaService service) {
        this.service = service;
    }

    @Override
    protected void doGet(HttpServletRequest requisicao, HttpServletResponse resposta) throws IOException {
        String rota = requisicao.getPathInfo() == null ? "/" : requisicao.getPathInfo();
        try {
            switch (rota) {
                case "/" -> responder(resposta, 200, service.buscarUltimas(inteiro(requisicao, "limite", 100)));
                case "/ultima" -> {
                    var ultima = service.buscarUltima();
                    if (ultima.isPresent()) {
                        responder(resposta, 200, ultima.get());
                    } else {
                        erro(resposta, 404, "Nenhuma leitura registrada ainda");
                    }
                }
                case "/periodo" -> responder(resposta, 200, service.buscarPorPeriodo(
                        dataHora(requisicao, "inicio"), dataHora(requisicao, "fim")));
                case "/total" -> responder(resposta, 200, Map.of("total", service.contar()));
                default -> erro(resposta, 404, "Endereço não encontrado: /api/leituras" + rota);
            }
        } catch (IllegalArgumentException e) {
            erro(resposta, 400, e.getMessage());
        } catch (PersistenciaException e) {
            log.severe("Falha ao consultar o banco de dados: " + e.getMessage());
            erro(resposta, 503, "Banco de dados indisponível no momento");
        } catch (RuntimeException e) {
            log.log(Level.SEVERE, "Erro inesperado em " + requisicao.getRequestURI(), e);
            erro(resposta, 500, "Erro interno no servidor");
        }
    }

    private int inteiro(HttpServletRequest requisicao, String nome, int padrao) {
        String valor = requisicao.getParameter(nome);
        if (valor == null || valor.isBlank()) {
            return padrao;
        }
        try {
            return Integer.parseInt(valor.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Valor inválido para o parâmetro '" + nome + "': " + valor);
        }
    }

    private LocalDateTime dataHora(HttpServletRequest requisicao, String nome) {
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

    private void erro(HttpServletResponse resposta, int status, String mensagem) throws IOException {
        responder(resposta, status, ErroResposta.de(status, mensagem));
    }

    private void responder(HttpServletResponse resposta, int status, Object corpo) throws IOException {
        resposta.setStatus(status);
        resposta.setContentType("application/json");
        resposta.setCharacterEncoding("UTF-8");
        resposta.setHeader("Cache-Control", "no-store");
        json.writeValue(resposta.getOutputStream(), corpo);
    }
}
