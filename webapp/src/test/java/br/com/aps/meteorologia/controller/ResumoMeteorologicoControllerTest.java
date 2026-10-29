package br.com.aps.meteorologia.controller;

import br.com.aps.meteorologia.dao.IEstacaoDAO;
import br.com.aps.meteorologia.dao.ResumoDiarioMeteorologicoDAO;
import br.com.aps.meteorologia.dao.ResumoHorarioMeteorologicoDAO;
import br.com.aps.meteorologia.model.Estacao;
import br.com.aps.meteorologia.model.ResumoHorarioMeteorologico;
import br.com.aps.meteorologia.model.TipoResumoDiario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Contratos HTTP de histórico/previsão: parâmetros inválidos viram 400 com mensagem, nunca 500. */
class ResumoMeteorologicoControllerTest {

    private final ResumoDiarioMeteorologicoDAO diarios = mock(ResumoDiarioMeteorologicoDAO.class);
    private final ResumoHorarioMeteorologicoDAO horarios = mock(ResumoHorarioMeteorologicoDAO.class);
    private final IEstacaoDAO estacoes = mock(IEstacaoDAO.class);
    private MockMvc mvc;

    @BeforeEach
    void preparar() {
        mvc = MockMvcBuilders.standaloneSetup(new ResumoMeteorologicoController(
                diarios, horarios, estacoes, "America/Sao_Paulo")).build();
    }

    private void comEstacaoAtiva() {
        Estacao estacao = mock(Estacao.class);
        when(estacao.getId()).thenReturn(1L);
        when(estacoes.buscarAtiva()).thenReturn(Optional.of(estacao));
    }

    @Test
    void previsaoValidaRetornaLista() throws Exception {
        comEstacaoAtiva();
        when(diarios.buscarPrevisoes(eq(1L), any(), anyInt())).thenReturn(List.of());
        mvc.perform(get("/api/resumos-diarios/previsoes").param("dias", "5")).andExpect(status().isOk());
    }

    @Test
    void previsaoDiariaRetornaChanceMaximaDeChuva() throws Exception {
        comEstacaoAtiva();
        br.com.aps.meteorologia.model.ResumoDiarioMeteorologico resumo = new br.com.aps.meteorologia.model.ResumoDiarioMeteorologico();
        resumo.setEstacaoId(1L);
        resumo.setData(java.time.LocalDate.of(2026, 10, 29));
        resumo.setTipo(TipoResumoDiario.PREVISAO);
        resumo.setProbabilidadeChuvaMaxima(72);
        resumo.setFonteDados("Open-Meteo");
        resumo.setAtualizadoEm(java.time.LocalDateTime.of(2026, 10, 28, 10, 0));
        when(diarios.buscarPrevisoes(eq(1L), any(), anyInt())).thenReturn(List.of(resumo));

        mvc.perform(get("/api/resumos-diarios/previsoes").param("dias", "7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].probabilidadeChuvaMaxima").value(72));
    }

    @Test
    void previsaoComDiasForaDoIntervaloRetorna400() throws Exception {
        comEstacaoAtiva();
        mvc.perform(get("/api/resumos-diarios/previsoes").param("dias", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value("dias deve estar entre 1 e 16"));
        mvc.perform(get("/api/resumos-diarios/previsoes").param("dias", "99")).andExpect(status().isBadRequest());
    }

    @Test
    void semEstacaoAtivaRetorna400() throws Exception {
        when(estacoes.buscarAtiva()).thenReturn(Optional.empty());
        mvc.perform(get("/api/resumos-diarios/previsoes"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value("Nenhuma estação ativa"));
    }

    @Test
    void resumoHorarioRetornaProbabilidadeDeChuva() throws Exception {
        comEstacaoAtiva();
        ResumoHorarioMeteorologico resumo = new ResumoHorarioMeteorologico();
        resumo.setDataHora(java.time.LocalDateTime.of(2026, 10, 28, 14, 0));
        resumo.setTipo(TipoResumoDiario.PREVISAO);
        resumo.setTemperatura(27.0);
        resumo.setProbabilidadeChuva(50);
        resumo.setFonteDados("Open-Meteo");
        resumo.setAtualizadoEm(java.time.LocalDateTime.of(2026, 10, 28, 10, 0));
        when(horarios.buscarPrevisoes(eq(1L), any(), any())).thenReturn(List.of(resumo));

        mvc.perform(get("/api/resumos-horarios/previsoes")
                        .param("inicio", "2026-10-28T00:00:00").param("fim", "2026-10-29T00:00:00"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].probabilidadeChuva").value(50));
    }
}
