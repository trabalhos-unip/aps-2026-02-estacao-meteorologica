package br.com.aps.meteorologia.service;

import br.com.aps.meteorologia.client.ClienteOpenMeteo;
import br.com.aps.meteorologia.client.FonteMeteorologicaException;
import br.com.aps.meteorologia.client.RespostaSerieOpenMeteo;
import br.com.aps.meteorologia.dao.IEstacaoDAO;
import br.com.aps.meteorologia.dao.ResumoDiarioMeteorologicoDAO;
import br.com.aps.meteorologia.dao.ResumoHorarioMeteorologicoDAO;
import br.com.aps.meteorologia.model.ResumoDiarioMeteorologico;
import br.com.aps.meteorologia.model.ResumoHorarioMeteorologico;
import br.com.aps.meteorologia.model.TipoResumoDiario;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
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
@ConditionalOnBean(ClienteOpenMeteo.class)
public class SincronizacaoHistoricoPrevisaoService {

    private static final Logger log = LoggerFactory.getLogger(SincronizacaoHistoricoPrevisaoService.class);
    private static final String FONTE_HISTORICO = "Open-Meteo (histórico da API)";
    private static final String FONTE_PREVISAO = "Open-Meteo (previsão da API)";

    private final ClienteOpenMeteo client;
    private final ResumoDiarioMeteorologicoDAO dao;
    private final ResumoHorarioMeteorologicoDAO daoHorario;
    private final IEstacaoDAO estacaoDAO;
    private final String nomeEstacao;
    private final double latitude;
    private final double longitude;
    private final ZoneId zona;
    private final int diasHistorico;
    private final int diasPrevisao;
    /** Última estação para a qual os resumos foram atualizados com sucesso. */
    private volatile Long estacaoSincronizada;

    public SincronizacaoHistoricoPrevisaoService(ClienteOpenMeteo client,
                                                  ResumoDiarioMeteorologicoDAO dao,
                                                  ResumoHorarioMeteorologicoDAO daoHorario,
                                                  IEstacaoDAO estacaoDAO,
                                                  @Value("${estacao.nome}") String nomeEstacao,
                                                  @Value("${openmeteo.latitude}") double latitude,
                                                  @Value("${openmeteo.longitude}") double longitude,
                                                  @Value("${openmeteo.timezone}") String timezone,
                                                  @Value("${sincronizacao.historico-dias:15}") int diasHistorico,
                                                  @Value("${sincronizacao.previsao-dias:7}") int diasPrevisao) {
        this.client = client;
        this.dao = dao;
        this.daoHorario = daoHorario;
        this.estacaoDAO = estacaoDAO;
        this.nomeEstacao = nomeEstacao;
        this.latitude = latitude;
        this.longitude = longitude;
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

    /**
     * Não espera a rotina de seis horas quando a pessoa troca a cidade na tela.
     * A próxima verificação encontra a nova estação ativa e carrega o histórico
     * e a previsão dela sem misturar dados com a estação anterior.
     */
    @Scheduled(fixedDelayString = "${sincronizacao.verificacao-estacao-ms:10000}",
            initialDelayString = "${sincronizacao.verificacao-estacao-ms:10000}")
    public void sincronizarQuandoEstacaoMudar() {
        try {
            var ativa = estacaoDAO.buscarAtiva();
            if (ativa.isPresent() && !ativa.get().getId().equals(estacaoSincronizada)) {
                sincronizar();
            }
        } catch (RuntimeException e) {
            log.warn("Verificação da estação ativa ignorada: {}", e.getMessage());
        }
    }

    public synchronized void sincronizar() {
        try {
            LocalDate hoje = LocalDate.now(zona);
            var estacao = estacaoDAO.buscarAtiva().orElseGet(() -> estacaoDAO.buscarOuCriar(nomeEstacao, latitude, longitude));
            Long estacaoId = estacao.getId();
            RespostaSerieOpenMeteo serie = client.buscarSerieHoraria(diasHistorico, diasPrevisao,
                    estacao.getLatitude(), estacao.getLongitude(), estacao.getFusoHorario());
            if (serie == null || serie.hourly() == null) {
                log.warn("Sincronização de histórico/previsão ignorada — série horária ausente");
                return;
            }
            Map<LocalDate, AcumuladoDiario> porDia = agrupar(serie.hourly());
            LocalDateTime agora = LocalDateTime.now(zona);
            List<ResumoHorarioMeteorologico> horarios = criarResumosHorarios(serie.hourly(), agora);
            int historicos = 0;
            int previsoes = 0;
            for (AcumuladoDiario acumulado : porDia.values()) {
                TipoResumoDiario tipo = acumulado.data.isBefore(hoje) ? TipoResumoDiario.HISTORICO : TipoResumoDiario.PREVISAO;
                dao.salvarOuAtualizar(acumulado.paraResumo(estacaoId, tipo, agora));
                if (tipo == TipoResumoDiario.HISTORICO) historicos++; else previsoes++;
            }
            dao.removerPrevisoesAnterioresA(estacaoId, hoje);
            for (ResumoHorarioMeteorologico horario : horarios) {
                horario.setEstacaoId(estacaoId);
                daoHorario.salvarOuAtualizar(horario);
            }
            daoHorario.removerPrevisoesAnterioresA(estacaoId, agora.truncatedTo(ChronoUnit.HOURS));
            estacaoSincronizada = estacaoId;
            log.info("Histórico e previsão sincronizados: {} dias históricos, {} dias previstos e {} pontos horários", historicos, previsoes, horarios.size());
        } catch (FonteMeteorologicaException e) {
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
                        valor(horas.precipitation(), indice), valor(horas.probabilidadeChuva(), indice),
                        valor(horas.velocidadeVento(), indice), valor(horas.codigoClima(), indice));
            } catch (DateTimeParseException e) {
                log.warn("Horário inesperado ignorado na série da Open-Meteo: {}", tempos.get(indice));
            }
        }
        return porDia;
    }

    private List<ResumoHorarioMeteorologico> criarResumosHorarios(RespostaSerieOpenMeteo.DadosHorarios horas, LocalDateTime agora) {
        List<ResumoHorarioMeteorologico> resumos = new ArrayList<>();
        LocalDateTime inicioDaHoraAtual = agora.truncatedTo(ChronoUnit.HOURS);
        for (int indice = 0; indice < horas.time().size(); indice++) {
            try {
                LocalDateTime momento = LocalDateTime.parse(horas.time().get(indice));
                TipoResumoDiario tipo = momento.isBefore(inicioDaHoraAtual) ? TipoResumoDiario.HISTORICO : TipoResumoDiario.PREVISAO;
                ResumoHorarioMeteorologico resumo = new ResumoHorarioMeteorologico();
                resumo.setDataHora(momento);
                resumo.setTipo(tipo);
                resumo.setTemperatura(valor(horas.temperatura(), indice));
                resumo.setUmidadeRelativa(valor(horas.umidadeRelativa(), indice));
                resumo.setPrecipitacao(valor(horas.precipitation(), indice));
                resumo.setChuva(valor(horas.rain(), indice));
                resumo.setProbabilidadeChuva(valor(horas.probabilidadeChuva(), indice));
                resumo.setVelocidadeVento(valor(horas.velocidadeVento(), indice));
                resumo.setCodigoClima(valor(horas.codigoClima(), indice));
                resumo.setFonteDados(tipo == TipoResumoDiario.HISTORICO ? FONTE_HISTORICO : FONTE_PREVISAO);
                resumo.setAtualizadoEm(agora);
                resumos.add(resumo);
            } catch (DateTimeParseException e) {
                log.warn("Horário inesperado ignorado na série da Open-Meteo: {}", horas.time().get(indice));
            }
        }
        return resumos;
    }

    private static <T> T valor(List<T> valores, int indice) {
        return valores != null && indice < valores.size() ? valores.get(indice) : null;
    }

    private static final class AcumuladoDiario {
        private final LocalDate data;
        private final List<Double> temperaturas = new ArrayList<>();
        private final List<Integer> umidades = new ArrayList<>();
        private final List<Double> precipitacoes = new ArrayList<>();
        private final List<Integer> probabilidadesChuva = new ArrayList<>();
        private final List<Double> ventos = new ArrayList<>();
        private Integer codigoClima;
        private Integer codigoClimaMeioDia;

        private AcumuladoDiario(LocalDate data) { this.data = data; }

        private void adicionar(LocalDateTime momento, Double temperatura, Integer umidade, Double precipitacao, Integer probabilidadeChuva,
                               Double vento, Integer codigo) {
            if (temperatura != null) temperaturas.add(temperatura);
            if (umidade != null) umidades.add(umidade);
            if (precipitacao != null) precipitacoes.add(precipitacao);
            if (probabilidadeChuva != null) probabilidadesChuva.add(probabilidadeChuva);
            if (vento != null) ventos.add(vento);
            if (codigo != null) {
                codigoClima = codigo;
                if (momento.getHour() == 12) codigoClimaMeioDia = codigo;
            }
        }

        private ResumoDiarioMeteorologico paraResumo(Long estacaoId, TipoResumoDiario tipo, LocalDateTime atualizadoEm) {
            ResumoDiarioMeteorologico resumo = new ResumoDiarioMeteorologico();
            resumo.setEstacaoId(estacaoId);
            resumo.setData(data);
            resumo.setTipo(tipo);
            resumo.setTemperaturaMinima(minimo(temperaturas));
            resumo.setTemperaturaMaxima(maximo(temperaturas));
            resumo.setTemperaturaMedia(media(temperaturas));
            resumo.setUmidadeMedia(mediaInteiros(umidades));
            resumo.setPrecipitacaoTotal(soma(precipitacoes));
            resumo.setProbabilidadeChuvaMaxima(maximoInteiros(probabilidadesChuva));
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
        private static Integer maximoInteiros(List<Integer> valores) { return valores.stream().max(Comparator.naturalOrder()).orElse(null); }
    }
}
