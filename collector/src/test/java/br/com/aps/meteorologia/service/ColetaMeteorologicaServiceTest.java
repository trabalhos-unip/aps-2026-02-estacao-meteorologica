package br.com.aps.meteorologia.service;

import br.com.aps.meteorologia.client.CenarioSimulado;
import br.com.aps.meteorologia.client.DadosMeteorologicosAtuais;
import br.com.aps.meteorologia.client.FonteMeteorologicaException;
import br.com.aps.meteorologia.client.IFonteMeteorologica;
import br.com.aps.meteorologia.client.SimuladorMeteorologico;
import br.com.aps.meteorologia.dao.IEstacaoDAO;
import br.com.aps.meteorologia.dao.ILeituraMeteorologicaDAO;
import br.com.aps.meteorologia.dao.ILogColetaDAO;
import br.com.aps.meteorologia.dao.PersistenciaException;
import br.com.aps.meteorologia.model.Estacao;
import br.com.aps.meteorologia.model.LeituraMeteorologica;
import br.com.aps.meteorologia.model.ModoOperacao;
import br.com.aps.meteorologia.model.TipoErroColeta;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ColetaMeteorologicaServiceTest {

    private static final LocalDateTime DATA_HORA_COLETA = LocalDateTime.of(2026, 10, 20, 22, 20, 5);
    private static final DadosMeteorologicosAtuais DADOS_METEOROLOGICOS = new DadosMeteorologicosAtuais(
            -23.514938, -46.610504, "2026-10-20T22:15", 14.0, 89, 0.0, 0.0, 9.8, 3,
            13.1, 1015.2, 180, 14.0, 45);

    private IFonteMeteorologica fonteMeteorologica;
    private ILeituraMeteorologicaDAO leituraMeteorologicaDAO;
    private IEstacaoDAO estacaoDAO;
    private ILogColetaDAO logColetaDAO;
    private ConversorLeituraMeteorologica conversorLeitura;
    private ColetaMeteorologicaService servicoColeta;

    @BeforeEach
    void preparar() {
        fonteMeteorologica = mock(IFonteMeteorologica.class);
        // A coleta agora informa as coordenadas da estação ativa. O mock mantém
        // o comportamento configurado pelos cenários legados de obterDadosAtuais().
        when(fonteMeteorologica.obterDadosAtuais(anyDouble(), anyDouble(), anyString()))
                .thenAnswer(chamada -> fonteMeteorologica.obterDadosAtuais());
        when(fonteMeteorologica.obterModoOperacao()).thenReturn(ModoOperacao.REAL);
        when(fonteMeteorologica.obterNomeFonte()).thenReturn(LeituraMeteorologica.FONTE_OPEN_METEO);

        leituraMeteorologicaDAO = mock(ILeituraMeteorologicaDAO.class);
        when(leituraMeteorologicaDAO.salvar(any())).thenAnswer(chamada -> chamada.getArgument(0));

        estacaoDAO = mock(IEstacaoDAO.class);
        Estacao estacao = new Estacao();
        estacao.setId(1L);
        when(estacaoDAO.buscarOuCriar(any(), any(Double.class), any(Double.class))).thenReturn(estacao);

        logColetaDAO = mock(ILogColetaDAO.class);
        conversorLeitura = new ConversorLeituraMeteorologica();
        servicoColeta = criarServico(fonteMeteorologica);
    }

    @Test
    void converteDadosParaLeitura() {
        LeituraMeteorologica leitura = conversorLeitura.converterParaLeitura(
                DADOS_METEOROLOGICOS, DATA_HORA_COLETA);

        assertThat(leitura.getDataHoraColeta()).isEqualTo(DATA_HORA_COLETA);
        assertThat(leitura.getDataHoraFonte()).isEqualTo(LocalDateTime.of(2026, 10, 20, 22, 15));
        assertThat(leitura.getTemperatura()).isEqualTo(14.0);
        assertThat(leitura.getUmidadeRelativa()).isEqualTo(89);
        assertThat(leitura.getPrecipitacao()).isEqualTo(0.0);
        assertThat(leitura.getChuva()).isEqualTo(0.0);
        assertThat(leitura.getVelocidadeVento()).isEqualTo(9.8);
        assertThat(leitura.getCodigoClima()).isEqualTo(3);
        assertThat(leitura.getSensacaoTermica()).isEqualTo(13.1);
        assertThat(leitura.getPressaoSuperficie()).isEqualTo(1015.2);
        assertThat(leitura.getDirecaoVento()).isEqualTo(180);
        assertThat(leitura.getRajadaVento()).isEqualTo(14.0);
        assertThat(leitura.getCoberturaNuvens()).isEqualTo(45);
        assertThat(leitura.getPresencaPrecipitacao()).isFalse();
    }

    @Test
    void coletaRealGravaCoordenadasModoEFonte() {
        when(fonteMeteorologica.obterDadosAtuais()).thenReturn(DADOS_METEOROLOGICOS);

        LeituraMeteorologica leitura = servicoColeta.coletarESalvarLeitura();

        assertThat(leitura.getLatitude()).isEqualTo(-23.514938);
        assertThat(leitura.getLongitude()).isEqualTo(-46.610504);
        assertThat(leitura.getEstacaoId()).isEqualTo(1L);
        assertThat(leitura.getModoOperacao()).isEqualTo(ModoOperacao.REAL);
        assertThat(leitura.getFonteDados()).isEqualTo("Open-Meteo");
        verify(leituraMeteorologicaDAO).salvar(leitura);
    }

    @Test
    void coletaSimuladaPassaPeloMesmoFluxo() {
        SimuladorMeteorologico simulador =
                new SimuladorMeteorologico("CHUVA_FORTE", -23.5505, -46.6333, "America/Sao_Paulo");

        LeituraMeteorologica leitura = criarServico(simulador).coletarESalvarLeitura();

        assertThat(leitura.getModoOperacao()).isEqualTo(ModoOperacao.SIMULADO);
        assertThat(leitura.getFonteDados()).isEqualTo("Simulador");
        assertThat(leitura.getLatitude()).isEqualTo(-23.5505);
        assertThat(leitura.getPrecipitacao()).isEqualTo(CenarioSimulado.CHUVA_FORTE.precipitacao());
        assertThat(leitura.getPresencaPrecipitacao()).isTrue();
        verify(leituraMeteorologicaDAO).salvar(leitura);
    }

    @Test
    void valoresAusentesPermanecemNulos() {
        DadosMeteorologicosAtuais dadosParciais = new DadosMeteorologicosAtuais(
                null, null, null, 20.5, null, null, null, null, null,
                null, null, null, null, null);

        LeituraMeteorologica leitura = conversorLeitura.converterParaLeitura(
                dadosParciais, DATA_HORA_COLETA);

        assertThat(leitura.getTemperatura()).isEqualTo(20.5);
        assertThat(leitura.getDataHoraFonte()).isNull();
        assertThat(leitura.getUmidadeRelativa()).isNull();
        assertThat(leitura.getPresencaPrecipitacao()).isNull();
    }

    @Test
    void rejeitaDadosSemVariaveisMeteorologicas() {
        DadosMeteorologicosAtuais dadosVazios = new DadosMeteorologicosAtuais(
                null, null, "2026-10-20T22:15", null, null, null, null, null, null,
                null, null, null, null, null);

        assertThatThrownBy(() -> conversorLeitura.converterParaLeitura(dadosVazios, DATA_HORA_COLETA))
                .isInstanceOf(FonteMeteorologicaException.class);
    }

    @Test
    void determinaPresencaDePrecipitacao() {
        assertThat(ConversorLeituraMeteorologica.determinarPresencaPrecipitacao(0.0, 0.0)).isFalse();
        assertThat(ConversorLeituraMeteorologica.determinarPresencaPrecipitacao(0.4, 0.0)).isTrue();
        assertThat(ConversorLeituraMeteorologica.determinarPresencaPrecipitacao(0.0, 0.2)).isTrue();
        assertThat(ConversorLeituraMeteorologica.determinarPresencaPrecipitacao(null, 1.0)).isTrue();
        assertThat(ConversorLeituraMeteorologica.determinarPresencaPrecipitacao(null, null)).isNull();
    }

    @Test
    void coletaAgendadaSalvaLeitura() {
        when(fonteMeteorologica.obterDadosAtuais()).thenReturn(DADOS_METEOROLOGICOS);

        servicoColeta.executarColetaAgendada();

        verify(leituraMeteorologicaDAO).salvar(any(LeituraMeteorologica.class));
    }

    @Test
    void falhaDaFonteNaoInterrompeOAgendamento() {
        when(fonteMeteorologica.obterDadosAtuais()).thenThrow(new FonteMeteorologicaException(
                "Fonte indisponível", TipoErroColeta.REDE));

        assertThatCode(servicoColeta::executarColetaAgendada).doesNotThrowAnyException();
        verify(leituraMeteorologicaDAO, never()).salvar(any());
    }

    @Test
    void falhaDoBancoNaoInterrompeOAgendamento() {
        when(fonteMeteorologica.obterDadosAtuais()).thenReturn(DADOS_METEOROLOGICOS);
        when(leituraMeteorologicaDAO.salvar(any()))
                .thenThrow(new PersistenciaException("MySQL indisponível", null));

        assertThatCode(servicoColeta::executarColetaAgendada).doesNotThrowAnyException();
    }

    private ColetaMeteorologicaService criarServico(IFonteMeteorologica fonte) {
        return new ColetaMeteorologicaService(
                fonte, leituraMeteorologicaDAO, estacaoDAO, logColetaDAO, conversorLeitura,
                "Estação Sorocaba", -23.5015, -47.4526, "America/Sao_Paulo");
    }
}
