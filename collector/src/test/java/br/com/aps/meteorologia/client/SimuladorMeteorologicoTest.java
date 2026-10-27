package br.com.aps.meteorologia.client;

import br.com.aps.meteorologia.model.ModoOperacao;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SimuladorMeteorologicoTest {

    private SimuladorMeteorologico simulador(String cenario) {
        return new SimuladorMeteorologico(cenario, -23.5505, -46.6333, "America/Sao_Paulo");
    }

    @Test
    void semCenarioAlternaTodosOsCenarios() {
        SimuladorMeteorologico simulador = simulador("");
        List<Integer> codigos = new ArrayList<>();
        for (int i = 0; i < CenarioSimulado.values().length; i++) {
            codigos.add(simulador.obterDadosAtuais().codigoClima());
        }

        assertThat(simulador.obterCenarios()).containsExactly(CenarioSimulado.values());
        assertThat(codigos).containsExactly(0, 65, 3, null);
    }

    @Test
    void cenarioFixoSempreRepete() {
        SimuladorMeteorologico simulador = simulador("vento_forte");

        for (int i = 0; i < 3; i++) {
            DadosMeteorologicosAtuais atuais = simulador.obterDadosAtuais();
            assertThat(atuais.velocidadeVento()).isBetween(60.0, 76.0);
            assertThat(atuais.precipitacao()).isZero();
        }
    }

    @Test
    void variaTemperaturaParaOGraficoNaoFicarPlano() {
        SimuladorMeteorologico simulador = simulador("TEMPO_SECO");

        double primeira = simulador.obterDadosAtuais().temperatura();
        double segunda = simulador.obterDadosAtuais().temperatura();

        assertThat(primeira).isNotEqualTo(segunda);
        assertThat(primeira).isBetween(30.0, 32.0);
    }

    @Test
    void retornaDadosNormalizados() {
        DadosMeteorologicosAtuais resposta = simulador("TEMPO_SECO").obterDadosAtuais();

        assertThat(resposta.latitude()).isEqualTo(-23.5505);
        assertThat(resposta.longitude()).isEqualTo(-46.6333);
        assertThat(resposta.dataHoraFonte()).matches("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}");
    }

    @Test
    void dadosIncompletosChegamComoAusentes() {
        DadosMeteorologicosAtuais atuais = simulador("DADOS_INCOMPLETOS").obterDadosAtuais();

        assertThat(atuais.temperatura()).isNotNull();
        assertThat(atuais.umidadeRelativa()).isNull();
        assertThat(atuais.precipitacao()).isNull();
        assertThat(atuais.velocidadeVento()).isNull();
    }

    @Test
    void modoEFonte() {
        assertThat(simulador("").obterModoOperacao()).isEqualTo(ModoOperacao.SIMULADO);
        assertThat(simulador("").obterNomeFonte()).isEqualTo("Simulador");
    }

    @Test
    void cenarioInvalido() {
        assertThatThrownBy(() -> simulador("NEVASCA"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("NEVASCA");
    }
}
