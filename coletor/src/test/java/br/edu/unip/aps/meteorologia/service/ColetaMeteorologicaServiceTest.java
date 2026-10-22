package br.edu.unip.aps.meteorologia.service;

import br.edu.unip.aps.meteorologia.client.ApiMeteorologicaClient;
import br.edu.unip.aps.meteorologia.client.ApiMeteorologicaException;
import br.edu.unip.aps.meteorologia.client.RespostaOpenMeteo;
import br.edu.unip.aps.meteorologia.client.RespostaOpenMeteo.CondicoesAtuais;
import br.edu.unip.aps.meteorologia.dao.LeituraMeteorologicaDAO;
import br.edu.unip.aps.meteorologia.model.LeituraMeteorologica;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import br.edu.unip.aps.meteorologia.dao.PersistenciaException;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ColetaMeteorologicaServiceTest {

    private static final LocalDateTime AGORA = LocalDateTime.of(2026, 10, 20, 22, 20, 5);
    private static final CondicoesAtuais CONDICOES =
            new CondicoesAtuais("2026-10-20T22:15", 900, 14.0, 89, 0.0, 0.0, 9.8, 3);

    private ApiMeteorologicaClient client;
    private LeituraMeteorologicaDAO dao;
    private ColetaMeteorologicaService service;

    @BeforeEach
    void preparar() {
        client = mock(ApiMeteorologicaClient.class);
        dao = mock(LeituraMeteorologicaDAO.class);
        when(dao.salvar(any())).thenAnswer(chamada -> chamada.getArgument(0));
        service = new ColetaMeteorologicaService(client, dao, "America/Sao_Paulo");
    }

    @Test
    void converteCondicoesParaLeitura() {
        LeituraMeteorologica leitura = service.converter(CONDICOES, AGORA);

        assertThat(leitura.getDataHoraColeta()).isEqualTo(AGORA);
        assertThat(leitura.getDataHoraFonte()).isEqualTo(LocalDateTime.of(2026, 10, 20, 22, 15));
        assertThat(leitura.getTemperatura()).isEqualTo(14.0);
        assertThat(leitura.getUmidadeRelativa()).isEqualTo(89);
        assertThat(leitura.getPrecipitacao()).isEqualTo(0.0);
        assertThat(leitura.getChuva()).isEqualTo(0.0);
        assertThat(leitura.getVelocidadeVento()).isEqualTo(9.8);
        assertThat(leitura.getCodigoClima()).isEqualTo(3);
        assertThat(leitura.getPresencaPrecipitacao()).isFalse();
        assertThat(leitura.getFonteDados()).isEqualTo("Open-Meteo");
    }

    @Test
    void valoresAusentesFicamNulos() {
        CondicoesAtuais parciais = new CondicoesAtuais(null, null, 20.5, null, null, null, null, null);

        LeituraMeteorologica leitura = service.converter(parciais, AGORA);

        assertThat(leitura.getTemperatura()).isEqualTo(20.5);
        assertThat(leitura.getDataHoraFonte()).isNull();
        assertThat(leitura.getUmidadeRelativa()).isNull();
        assertThat(leitura.getPresencaPrecipitacao()).isNull();
    }

    @Test
    void rejeitaRespostaSemVariaveis() {
        CondicoesAtuais vazias = new CondicoesAtuais("2026-10-20T22:15", 900, null, null, null, null, null, null);

        assertThatThrownBy(() -> service.converter(vazias, AGORA)).isInstanceOf(ApiMeteorologicaException.class);
    }

    @Test
    void regraDePresencaDePrecipitacao() {
        assertThat(ColetaMeteorologicaService.calcularPresencaPrecipitacao(0.0, 0.0)).isFalse();
        assertThat(ColetaMeteorologicaService.calcularPresencaPrecipitacao(0.4, 0.0)).isTrue();
        assertThat(ColetaMeteorologicaService.calcularPresencaPrecipitacao(0.0, 0.2)).isTrue();
        assertThat(ColetaMeteorologicaService.calcularPresencaPrecipitacao(null, 1.0)).isTrue();
        assertThat(ColetaMeteorologicaService.calcularPresencaPrecipitacao(null, null)).isNull();
    }

    @Test
    void coletaSalvaPeloDao() {
        when(client.buscarCondicoesAtuais()).thenReturn(new RespostaOpenMeteo(-23.5, -46.6, "America/Sao_Paulo", CONDICOES));

        service.executarColeta();

        verify(dao).salvar(any(LeituraMeteorologica.class));
    }

    @Test
    void falhaDaApiNaoDerrubaOColetor() {
        when(client.buscarCondicoesAtuais()).thenThrow(new ApiMeteorologicaException("API fora do ar"));

        assertThatCode(service::executarColeta).doesNotThrowAnyException();
        verify(dao, never()).salvar(any());
    }

    @Test
    void falhaDoBancoNaoDerrubaOColetor() {
        when(client.buscarCondicoesAtuais()).thenReturn(new RespostaOpenMeteo(-23.5, -46.6, "America/Sao_Paulo", CONDICOES));
        when(dao.salvar(any())).thenThrow(new PersistenciaException("MySQL indisponível", null));

        assertThatCode(service::executarColeta).doesNotThrowAnyException();
    }
}
