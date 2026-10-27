package br.edu.unip.aps.meteorologia.service;

import br.edu.unip.aps.meteorologia.client.ApiMeteorologicaClient;
import br.edu.unip.aps.meteorologia.client.ApiMeteorologicaException;
import br.edu.unip.aps.meteorologia.client.RespostaSerieOpenMeteo;
import br.edu.unip.aps.meteorologia.dao.ResumoDiarioMeteorologicoDAO;
import br.edu.unip.aps.meteorologia.model.ResumoDiarioMeteorologico;
import br.edu.unip.aps.meteorologia.model.TipoResumoDiario;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Preenche o banco imediatamente, sem esperar dias de coleta local. Os dias
 * concluídos recebem o tipo HISTORICO; hoje e os próximos dias recebem PREVISAO.
 */
@Service
public class SincronizacaoHistoricoPrevisaoService {

    private static final Logger log = LoggerFactory.getLogger(SincronizacaoHistoricoPrevisaoService.class);
    private static final String FONTE_HISTORICO = "Open-Meteo (histórico da API)";
    private static final String FONTE_PREVISAO = "Open-Meteo (previsão da API)";

    private final ApiMeteorologicaClient client;
    private final ResumoDiarioMeteorologicoDAO dao;
    private final ZoneId zona;
    private final int diasHistorico;
    private final int diasPrevisao;

    public SincronizacaoHistoricoPrevisaoService(ApiMeteorologicaClient client,
                                                  ResumoDiarioMeteorologicoDAO dao,
                                                  @Value("${openmeteo.timezone}") String timezone,
                                                  @Value("${sincronizacao.historico-dias:15}") int diasHistorico,
                                                  @Value("${sincronizacao.previsao-dias:7}") int diasPrevisao) {
        this.client = client;
        this.dao = dao;
        this.zona = ZoneId.of(timezone);
        this.diasHistorico = diasHistorico;
        this.diasPrevisao = diasPrevisao;
    }

    /** A primeira carga ocorre assim que o coletor fica pronto. */
    @EventListener(ApplicationReadyEvent.class)
    public void sincronizarAoIniciar() {
        sincronizar();
    }

    /** Atualiza previsões sem aumentar a frequência normal das leituras instantâneas. */
    @Scheduled(fixedDelayString = "${sincronizacao.intervalo-ms:21600000}", initialDelayString = "${sincronizacao.intervalo-ms:21600000}")
    public void sincronizarAgendado() {
        sincronizar();
    }

    public synchronized void sincronizar() {
        try {
            LocalDate hoje = LocalDate.now(zona);
            RespostaSerieOpenMeteo serie = client.buscarSerieHoraria(diasHistorico, diasPrevisao);
            Map<LocalDate, AcumuladoDiario> porDia = agrupar(serie.hourly());
            LocalDateTime agora = LocalDateTime.now(zona);
            int historicos = 0;
            int previsoes = 0;
            for (AcumuladoDiario acumulado : porDia.values()) {
                TipoResumoDiario tipo = acumulado.data.isBefore(hoje) ? TipoResumoDiario.HISTORICO : TipoResumoDiario.PREVISAO;
                dao.salvarOuAtualizar(acumulado.paraResumo(tipo, agora));
                if (tipo == TipoResumoDiario.HISTORICO) historicos++; else previsoes++;
            }
            dao.removerPrevisoesAnterioresA(hoje);
            log.info("Histórico e previsão sincronizados: {} dias históricos e {} dias previstos", historicos, previsoes);
        } catch (ApiMeteorologicaException e) {
            log.warn("Sincronização de histórico/previsão ignorada — falha na API: {}", e.getMessage());
        } catch (RuntimeException e) {
            log.error("Sincronização de histórico/previsão ignorada", e);
        }
    }

    private Map<LocalDate, AcumuladoDiario> agrupar(RespostaSerieOpenMeteo.DadosHorarios horas) {
        Map<LocalDate, AcumuladoDiario> porDia = new LinkedHashMap<>();
        List<String> tempos = horas.time();
        for (int indice = 0; indice < tempos.size(); indice++) {
            try {
                LocalDateTime momento = LocalDateTime.parse(tempos.get(indice));
                AcumuladoDiario acumulado = porDia.computeIfAbsent(momento.toLocalDate(), AcumuladoDiario::new);
                acumulado.adicionar(momento, valor(horas.temperatura(), indice), valor(horas.umidadeRelativa(), indice),
                        valor(horas.precipitation(), indice), valor(horas.velocidadeVento(), indice), valor(horas.codigoClima(), indice));
            } catch (DateTimeParseException e) {
                log.warn("Horário inesperado ignorado na série da Open-Meteo: {}", tempos.get(indice));
            }
        }
        return porDia;
    }

    private static <T> T valor(List<T> valores, int indice) {
        return valores != null && indice < valores.size() ? valores.get(indice) : null;
    }

    private static final class AcumuladoDiario {
        private final LocalDate data;
        private final List<Double> temperaturas = new ArrayList<>();
        private final List<Integer> umidades = new ArrayList<>();
        private final List<Double> precipitacoes = new ArrayList<>();
        private final List<Double> ventos = new ArrayList<>();
        private Integer codigoClima;
        private Integer codigoClimaMeioDia;

        private AcumuladoDiario(LocalDate data) { this.data = data; }

        private void adicionar(LocalDateTime momento, Double temperatura, Integer umidade, Double precipitacao,
                               Double vento, Integer codigo) {
            if (temperatura != null) temperaturas.add(temperatura);
            if (umidade != null) umidades.add(umidade);
            if (precipitacao != null) precipitacoes.add(precipitacao);
            if (vento != null) ventos.add(vento);
            if (codigo != null) {
                codigoClima = codigo;
                if (momento.getHour() == 12) codigoClimaMeioDia = codigo;
            }
        }

        private ResumoDiarioMeteorologico paraResumo(TipoResumoDiario tipo, LocalDateTime atualizadoEm) {
            ResumoDiarioMeteorologico resumo = new ResumoDiarioMeteorologico();
            resumo.setData(data);
            resumo.setTipo(tipo);
            resumo.setTemperaturaMinima(minimo(temperaturas));
            resumo.setTemperaturaMaxima(maximo(temperaturas));
            resumo.setTemperaturaMedia(media(temperaturas));
            resumo.setUmidadeMedia(mediaInteiros(umidades));
            resumo.setPrecipitacaoTotal(soma(precipitacoes));
            resumo.setVentoMaximo(maximo(ventos));
            resumo.setCodigoClima(codigoClimaMeioDia != null ? codigoClimaMeioDia : codigoClima);
            resumo.setFonteDados(tipo == TipoResumoDiario.HISTORICO ? FONTE_HISTORICO : FONTE_PREVISAO);
            resumo.setAtualizadoEm(atualizadoEm);
            return resumo;
        }

        private static Double minimo(List<Double> valores) { return valores.stream().min(Comparator.naturalOrder()).orElse(null); }
        private static Double maximo(List<Double> valores) { return valores.stream().max(Comparator.naturalOrder()).orElse(null); }
        private static Double soma(List<Double> valores) { return valores.isEmpty() ? null : valores.stream().mapToDouble(Double::doubleValue).sum(); }
        private static Double media(List<Double> valores) { return valores.isEmpty() ? null : valores.stream().mapToDouble(Double::doubleValue).average().orElseThrow(); }
        private static Double mediaInteiros(List<Integer> valores) { return valores.isEmpty() ? null : valores.stream().mapToInt(Integer::intValue).average().orElseThrow(); }
    }
}
