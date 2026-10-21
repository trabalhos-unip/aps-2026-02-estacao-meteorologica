package br.edu.unip.aps.meteorologia.service;

import br.edu.unip.aps.meteorologia.client.ApiMeteorologicaClient;
import br.edu.unip.aps.meteorologia.client.ApiMeteorologicaException;
import br.edu.unip.aps.meteorologia.client.RespostaOpenMeteo.CondicoesAtuais;
import br.edu.unip.aps.meteorologia.dao.LeituraMeteorologicaDAO;
import br.edu.unip.aps.meteorologia.model.LeituraMeteorologica;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.TransactionException;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;

/**
 * Caso de uso da coleta: obtém os dados da API, converte para {@link LeituraMeteorologica}
 * e salva pelo DAO. Executado a cada {@code coleta.intervalo-ms} (10 segundos, conforme o enunciado).
 *
 * <p>A Open-Meteo atualiza as condições atuais a cada 15 minutos; por isso várias coletas seguidas
 * podem ter o mesmo {@code dataHoraFonte} e os mesmos valores. Isso é esperado.
 */
@Service
public class ColetaMeteorologicaService {

    private static final Logger log = LoggerFactory.getLogger(ColetaMeteorologicaService.class);

    private final ApiMeteorologicaClient client;
    private final LeituraMeteorologicaDAO dao;
    private final ZoneId zona;

    public ColetaMeteorologicaService(ApiMeteorologicaClient client,
                                      LeituraMeteorologicaDAO dao,
                                      @Value("${openmeteo.timezone}") String timezone) {
        this.client = client;
        this.dao = dao;
        this.zona = ZoneId.of(timezone);
    }

    /**
     * Ciclo agendado. Nenhuma falha recuperável interrompe o coletor: o erro é registrado no log
     * e a próxima coleta acontece normalmente.
     */
    @Scheduled(fixedDelayString = "${coleta.intervalo-ms:10000}")
    public void executarColeta() {
        try {
            LeituraMeteorologica leitura = coletar();
            log.info("Leitura #{} salva | coleta={} fonte={} | {} °C, {} %, precip. {} mm, chuva {} mm, vento {} km/h, código WMO {}",
                    leitura.getId(), leitura.getDataHoraColeta(), leitura.getDataHoraFonte(),
                    leitura.getTemperatura(), leitura.getUmidadeRelativa(), leitura.getPrecipitacao(),
                    leitura.getChuva(), leitura.getVelocidadeVento(), leitura.getCodigoClima());
        } catch (ApiMeteorologicaException e) {
            log.warn("Coleta ignorada — falha na API: {}", e.getMessage());
        } catch (DataAccessException | TransactionException e) {
            log.error("Coleta ignorada — falha ao gravar no banco: {}", e.getMostSpecificCause().getMessage());
        } catch (RuntimeException e) {
            log.error("Coleta ignorada — erro inesperado", e);
        }
    }

    /** Executa uma coleta completa (API → conversão → banco) e devolve a leitura gravada. */
    public LeituraMeteorologica coletar() {
        CondicoesAtuais atuais = client.buscarCondicoesAtuais().current();
        LeituraMeteorologica leitura = converter(atuais, LocalDateTime.now(zona));
        return dao.salvar(leitura);
    }

    /** Converte as condições da API para o modelo interno. Valores ausentes ficam {@code null}. */
    LeituraMeteorologica converter(CondicoesAtuais atuais, LocalDateTime dataHoraColeta) {
        if (atuais.temperatura() == null && atuais.umidadeRelativa() == null
                && atuais.precipitacao() == null && atuais.velocidadeVento() == null) {
            throw new ApiMeteorologicaException("Resposta da Open-Meteo sem nenhuma variável meteorológica");
        }
        LeituraMeteorologica leitura = new LeituraMeteorologica();
        leitura.setDataHoraColeta(dataHoraColeta);
        leitura.setDataHoraFonte(converterDataHora(atuais.time()));
        leitura.setTemperatura(atuais.temperatura());
        leitura.setUmidadeRelativa(atuais.umidadeRelativa());
        leitura.setPrecipitacao(atuais.precipitacao());
        leitura.setChuva(atuais.chuva());
        leitura.setVelocidadeVento(atuais.velocidadeVento());
        leitura.setCodigoClima(atuais.codigoClima());
        leitura.setPresencaPrecipitacao(calcularPresencaPrecipitacao(atuais.precipitacao(), atuais.chuva()));
        leitura.setFonteDados(LeituraMeteorologica.FONTE_OPEN_METEO);
        return leitura;
    }

    /**
     * Regra do grupo para "presença de precipitação": há precipitação quando o valor de
     * {@code precipitation} ou de {@code rain} é maior que zero. Sem nenhum dos dois, fica {@code null}.
     */
    static Boolean calcularPresencaPrecipitacao(Double precipitacao, Double chuva) {
        if (precipitacao == null && chuva == null) {
            return null;
        }
        return (precipitacao != null && precipitacao > 0) || (chuva != null && chuva > 0);
    }

    /** {@code current.time} vem no formato ISO local, ex.: "2026-10-20T22:15". */
    private LocalDateTime converterDataHora(String time) {
        if (time == null || time.isBlank()) {
            return null;
        }
        try {
            return LocalDateTime.parse(time);
        } catch (DateTimeParseException e) {
            log.warn("Horário da fonte em formato inesperado: '{}'", time);
            return null;
        }
    }
}
