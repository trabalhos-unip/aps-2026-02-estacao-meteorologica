package br.edu.unip.aps.meteorologia;

import br.edu.unip.aps.meteorologia.client.ApiMeteorologicaClient;
import br.edu.unip.aps.meteorologia.config.Configuracao;
import br.edu.unip.aps.meteorologia.config.HibernateUtil;
import br.edu.unip.aps.meteorologia.config.Logs;
import br.edu.unip.aps.meteorologia.dao.LeituraMeteorologicaDAO;
import br.edu.unip.aps.meteorologia.dao.impl.LeituraMeteorologicaDAOImpl;
import br.edu.unip.aps.meteorologia.service.ColetaMeteorologicaService;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 * Aplicação 1 — Coletor: API Open-Meteo → ApiMeteorologicaClient → ColetaMeteorologicaService
 * → LeituraMeteorologicaDAO → Hibernate → MySQL.
 *
 * <p>Monta os objetos e agenda a coleta a cada {@code COLETA_INTERVALO_MS} (10 segundos por padrão).
 * Para encerrar, Ctrl+C (ou o botão Stop do IntelliJ).
 */
public class ColetorApplication {

    public static final String TIMEZONE = "America/Sao_Paulo";

    public static void main(String[] args) {
        Logs.configurar();
        Logger log = Logger.getLogger(ColetorApplication.class.getName());

        ApiMeteorologicaClient client = new ApiMeteorologicaClient(
                new ObjectMapper(),
                Configuracao.texto("OPENMETEO_URL", "https://api.open-meteo.com/v1/forecast"),
                Configuracao.decimal("OPENMETEO_LATITUDE", -23.5505),
                Configuracao.decimal("OPENMETEO_LONGITUDE", -46.6333),
                TIMEZONE,
                Configuracao.inteiro("OPENMETEO_TIMEOUT_SEGUNDOS", 10));
        LeituraMeteorologicaDAO dao = new LeituraMeteorologicaDAOImpl();
        ColetaMeteorologicaService service = new ColetaMeteorologicaService(client, dao, TIMEZONE);

        int intervaloMs = Configuracao.inteiro("COLETA_INTERVALO_MS", 10_000);
        ScheduledExecutorService agendador = Executors.newSingleThreadScheduledExecutor();
        agendador.scheduleWithFixedDelay(service::executarColeta, 0, intervaloMs, TimeUnit.MILLISECONDS);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.info("Encerrando o coletor...");
            agendador.shutdown();
            HibernateUtil.fechar();
        }));
        log.info("Coletor iniciado: uma coleta a cada " + intervaloMs + " ms em " + client.getUri());
    }
}
