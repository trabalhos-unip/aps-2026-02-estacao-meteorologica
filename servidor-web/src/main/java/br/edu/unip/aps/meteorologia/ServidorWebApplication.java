package br.edu.unip.aps.meteorologia;

import br.edu.unip.aps.meteorologia.config.Configuracao;
import br.edu.unip.aps.meteorologia.config.HibernateUtil;
import br.edu.unip.aps.meteorologia.config.Logs;
import br.edu.unip.aps.meteorologia.controller.LeituraMeteorologicaServlet;
import br.edu.unip.aps.meteorologia.dao.impl.LeituraMeteorologicaDAOImpl;
import br.edu.unip.aps.meteorologia.service.ConsultaMeteorologicaService;
import org.apache.catalina.Context;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.servlets.DefaultServlet;
import org.apache.catalina.startup.Tomcat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.logging.Logger;

/**
 * Aplicação 2 — Web: Navegador → LeituraMeteorologicaServlet → ConsultaMeteorologicaService
 * → LeituraMeteorologicaDAO → Hibernate → MySQL.
 *
 * <p>Sobe um servidor Tomcat embutido (não é preciso instalar Tomcat) que:
 * <ul>
 *   <li>entrega os arquivos da pasta {@code servidor-web/src/main/webapp} (index.html, css, js);</li>
 *   <li>atende {@code /api/leituras/*} pelo {@link LeituraMeteorologicaServlet}.</li>
 * </ul>
 */
public class ServidorWebApplication {

    public static void main(String[] args) throws Exception {
        Logs.configurar();
        Logger log = Logger.getLogger(ServidorWebApplication.class.getName());

        int porta = Configuracao.inteiro("SERVIDOR_PORTA", 8080);
        ConsultaMeteorologicaService service = new ConsultaMeteorologicaService(new LeituraMeteorologicaDAOImpl());
        Tomcat tomcat = criarServidor(porta, service);
        tomcat.start();

        Runtime.getRuntime().addShutdownHook(new Thread(HibernateUtil::fechar));
        log.info("Servidor web iniciado em http://localhost:" + porta);
        tomcat.getServer().await();
    }

    /** Monta o Tomcat com a página estática e o Servlet da API. Porta 0 = porta livre qualquer (testes). */
    public static Tomcat criarServidor(int porta, ConsultaMeteorologicaService service)
            throws IOException, LifecycleException {
        Tomcat tomcat = new Tomcat();
        tomcat.setBaseDir(Files.createTempDirectory("tomcat-estacao").toString());
        tomcat.setPort(porta);
        tomcat.getConnector();

        Context contexto = tomcat.addContext("", localizarPastaWebapp().toAbsolutePath().toString());
        Tomcat.addDefaultMimeTypeMappings(contexto);
        contexto.addWelcomeFile("index.html");

        Tomcat.addServlet(contexto, "paginas", new DefaultServlet());
        contexto.addServletMappingDecoded("/", "paginas");

        Tomcat.addServlet(contexto, "leituras", new LeituraMeteorologicaServlet(service));
        contexto.addServletMappingDecoded("/api/leituras/*", "leituras");
        return tomcat;
    }

    /** Funciona rodando pela raiz do projeto (IntelliJ) ou pela pasta do módulo (Maven). */
    private static Path localizarPastaWebapp() {
        for (Path candidato : List.of(Path.of("servidor-web", "src", "main", "webapp"), Path.of("src", "main", "webapp"))) {
            if (Files.isDirectory(candidato)) {
                return candidato;
            }
        }
        throw new IllegalStateException("Pasta webapp não encontrada. Execute a partir da raiz do projeto.");
    }
}
