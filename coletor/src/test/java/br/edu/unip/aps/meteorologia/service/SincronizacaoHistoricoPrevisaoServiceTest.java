package br.edu.unip.aps.meteorologia.service;

import br.edu.unip.aps.meteorologia.client.ApiMeteorologicaClient;
import br.edu.unip.aps.meteorologia.client.RespostaSerieOpenMeteo;
import br.edu.unip.aps.meteorologia.dao.ResumoDiarioMeteorologicoDAO;
import br.edu.unip.aps.meteorologia.model.ResumoDiarioMeteorologico;
import br.edu.unip.aps.meteorologia.model.TipoResumoDiario;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SincronizacaoHistoricoPrevisaoServiceTest {

    @Test
    void separaDiasPassadosDePrevisoesECalculaResumo() {
        ApiMeteorologicaClient client = mock(ApiMeteorologicaClient.class);
        ResumoDiarioMeteorologicoDAO dao = mock(ResumoDiarioMeteorologicoDAO.class);
        ZoneId zona = ZoneId.of("America/Sao_Paulo");
        LocalDate ontem = LocalDate.now(zona).minusDays(1);
        LocalDate hoje = LocalDate.now(zona);
        DateTimeFormatter formato = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");
        RespostaSerieOpenMeteo.DadosHorarios horas = new RespostaSerieOpenMeteo.DadosHorarios(
                List.of(ontem.atTime(12, 0).format(formato), ontem.atTime(13, 0).format(formato), hoje.atTime(12, 0).format(formato)),
                List.of(20.0, 24.0, 22.0), List.of(80, 60, 70), List.of(0.0, 1.2, 0.4),
                List.of(0.0, 1.2, 0.4), List.of(8.0, 14.0, 10.0), List.of(3, 61, 2));
        when(client.buscarSerieHoraria(15, 7)).thenReturn(new RespostaSerieOpenMeteo("America/Sao_Paulo", horas));
        SincronizacaoHistoricoPrevisaoService service = new SincronizacaoHistoricoPrevisaoService(client, dao,
                "America/Sao_Paulo", 15, 7);

        service.sincronizar();

        ArgumentCaptor<ResumoDiarioMeteorologico> captor = ArgumentCaptor.forClass(ResumoDiarioMeteorologico.class);
        verify(dao, org.mockito.Mockito.times(2)).salvarOuAtualizar(captor.capture());
        assertThat(captor.getAllValues()).extracting(ResumoDiarioMeteorologico::getTipo)
                .containsExactly(TipoResumoDiario.HISTORICO, TipoResumoDiario.PREVISAO);
        ResumoDiarioMeteorologico historico = captor.getAllValues().getFirst();
        assertThat(historico.getTemperaturaMinima()).isEqualTo(20.0);
        assertThat(historico.getTemperaturaMaxima()).isEqualTo(24.0);
        assertThat(historico.getTemperaturaMedia()).isEqualTo(22.0);
        assertThat(historico.getPrecipitacaoTotal()).isEqualTo(1.2);
        verify(dao).removerPrevisoesAnterioresA(hoje);
    }
}
