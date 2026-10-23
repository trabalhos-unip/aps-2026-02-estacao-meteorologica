package br.com.aps.meteorologia.client;

import br.com.aps.meteorologia.model.LeituraMeteorologica;
import br.com.aps.meteorologia.model.ModoOperacao;
import br.com.aps.meteorologia.util.DataHoraUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Fonte SIMULADA: gera condições meteorológicas de cenários de teste, sem acessar a internet.
 * Ativada com {@code coleta.modo=SIMULADO} (variável {@code COLETA_MODO=SIMULADO} no .env).
 *
 * <ul>
 *   <li>Com {@code coleta.cenario} definido (ex.: CHUVA_FORTE), repete sempre esse cenário.</li>
 *   <li>Sem cenário definido, alterna: TEMPO_SECO → CHUVA_FORTE → VENTO_FORTE → DADOS_INCOMPLETOS → ...</li>
 * </ul>
 * A cada coleta aplica uma pequena variação na temperatura e no vento, para o gráfico não ficar plano.
 * As leituras são gravadas com modoOperacao = SIMULADO e fonteDados = "Simulador".
 */
@Component
@ConditionalOnProperty(name = "coleta.modo", havingValue = "SIMULADO")
public class SimuladorMeteorologico implements FonteMeteorologica {

    /** Variação aplicada a cada coleta (repete a cada 5 coletas): -0.4, -0.2, 0, +0.2, +0.4. */
    private static final double[] VARIACAO = {-0.4, -0.2, 0.0, 0.2, 0.4};

    private final List<CenarioSimulado> cenarios;
    private final double latitude;
    private final double longitude;
    private final String timezone;
    private int contador;

    public SimuladorMeteorologico(@Value("${coleta.cenario:}") String cenario,
                                  @Value("${openmeteo.latitude}") double latitude,
                                  @Value("${openmeteo.longitude}") double longitude,
                                  @Value("${openmeteo.timezone}") String timezone) {
        this.cenarios = cenario == null || cenario.isBlank()
                ? List.of(CenarioSimulado.values())
                : List.of(converterCenario(cenario));
        this.latitude = latitude;
        this.longitude = longitude;
        this.timezone = timezone;
    }

    @Override
    public synchronized RespostaOpenMeteo buscarCondicoesAtuais() {
        CenarioSimulado cenario = cenarios.get(contador % cenarios.size());
        double variacao = VARIACAO[contador % VARIACAO.length];
        contador++;

        String horario = DataHoraUtil.agora(ZoneId.of(timezone)).truncatedTo(ChronoUnit.MINUTES).toString();
        RespostaOpenMeteo.CondicoesAtuais atuais = new RespostaOpenMeteo.CondicoesAtuais(
                horario, 900,
                somar(cenario.temperatura(), variacao),
                cenario.umidadeRelativa(),
                cenario.precipitacao(),
                cenario.chuva(),
                somar(cenario.velocidadeVento(), variacao * 5),
                cenario.codigoClima());
        return new RespostaOpenMeteo(latitude, longitude, timezone, atuais);
    }

    @Override
    public ModoOperacao modo() {
        return ModoOperacao.SIMULADO;
    }

    @Override
    public String nomeFonte() {
        return LeituraMeteorologica.FONTE_SIMULADOR;
    }

    /** Cenários usados por este simulador, na ordem em que são gerados. */
    public List<CenarioSimulado> getCenarios() {
        return cenarios;
    }

    private static Double somar(Double valor, double variacao) {
        return valor == null ? null : Math.round((valor + variacao) * 10) / 10.0;
    }

    private static CenarioSimulado converterCenario(String texto) {
        try {
            return CenarioSimulado.valueOf(texto.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Cenário inválido em coleta.cenario: '" + texto
                    + "'. Use um destes: " + List.of(CenarioSimulado.values()), e);
        }
    }
}
