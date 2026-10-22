package br.edu.unip.aps.meteorologia.service;

import br.edu.unip.aps.meteorologia.client.ApiMeteorologicaClient;
import br.edu.unip.aps.meteorologia.client.ApiMeteorologicaException;
import br.edu.unip.aps.meteorologia.client.RespostaOpenMeteo.CondicoesAtuais;
import br.edu.unip.aps.meteorologia.dao.LeituraMeteorologicaDAO;
import br.edu.unip.aps.meteorologia.dao.PersistenciaException;
import br.edu.unip.aps.meteorologia.model.LeituraMeteorologica;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Caso de uso da coleta: obtém os dados da API, converte para {@link LeituraMeteorologica}
 * e salva pelo DAO. O {@code ColetorApplication} chama {@link #executarColeta()} a cada 10 segundos.
 *
 * <p>A Open-Meteo atualiza as condições atuais a cada 15 minutos; por isso várias coletas seguidas
 * podem ter o mesmo {@code dataHoraFonte} e os mesmos valores. Isso é esperado.
 */
public class ColetaMeteorologicaService {

    private static final Logger log = Logger.getLogger(ColetaMeteorologicaService.class.getName());

    private final ApiMeteorologicaClient client;
    private final LeituraMeteorologicaDAO dao;
    private final ZoneId zona;

    public ColetaMeteorologicaService(ApiMeteorologicaClient client,
                                      LeituraMeteorologicaDAO dao,
                                      String timezone) {
        this.client = client;
        this.dao = dao;
        this.zona = ZoneId.of(timezone);
    }

    /**
     * Ciclo agendado. Nenhuma falha recuperável interrompe o coletor: o erro é registrado no log
     * e a próxima coleta acontece normalmente.
     */
    public void executarColeta() {
        try {
            LeituraMeteorologica leitura = coletar();
            log.info(String.format("Leitura #%d salva | coleta=%s fonte=%s | %s °C, %s %%, precip. %s mm, chuva %s mm, vento %s km/h, código WMO %s",
                    leitura.getId(), leitura.getDataHoraColeta(), leitura.getDataHoraFonte(),
                    leitura.getTemperatura(), leitura.getUmidadeRelativa(), leitura.getPrecipitacao(),
                    leitura.getChuva(), leitura.getVelocidadeVento(), leitura.getCodigoClima()));
        } catch (ApiMeteorologicaException e) {
            log.warning("Coleta ignorada — falha na API: " + e.getMessage());
        } catch (PersistenciaException e) {
            log.severe("Coleta ignorada — falha no banco de dados: " + e.getMessage());
        } catch (RuntimeException e) {
            log.log(Level.SEVERE, "Coleta ignorada — erro inesperado", e);
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
            log.warning("Horário da fonte em formato inesperado: '" + time + "'");
            return null;
        }
    }
}
