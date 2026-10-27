package br.edu.unip.aps.meteorologia.controller;

import br.edu.unip.aps.meteorologia.dao.ResumoDiarioMeteorologicoDAO;
import br.edu.unip.aps.meteorologia.model.ResumoDiarioMeteorologico;
import br.edu.unip.aps.meteorologia.model.TipoResumoDiario;
import br.edu.unip.aps.meteorologia.service.ConsultaResumoDiarioService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ResumoDiarioMeteorologicoController.class)
@Import(ConsultaResumoDiarioService.class)
@TestPropertySource(properties = "openmeteo.timezone=America/Sao_Paulo")
class ResumoDiarioMeteorologicoControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private ResumoDiarioMeteorologicoDAO dao;

    private ResumoDiarioMeteorologico resumo(LocalDate data, TipoResumoDiario tipo) {
        ResumoDiarioMeteorologico resumo = new ResumoDiarioMeteorologico();
        resumo.setId(4L);
        resumo.setData(data);
        resumo.setTipo(tipo);
        resumo.setTemperaturaMinima(18.0);
        resumo.setTemperaturaMaxima(26.0);
        resumo.setTemperaturaMedia(22.0);
        resumo.setPrecipitacaoTotal(2.4);
        resumo.setCodigoClima(61);
        resumo.setFonteDados("Open-Meteo (teste)");
        resumo.setAtualizadoEm(LocalDateTime.of(2026, 10, 27, 10, 0));
        return resumo;
    }

    @Test
    void entregaHistoricoComTipoExplicito() throws Exception {
        when(dao.buscarHistorico(any(), any())).thenReturn(List.of(resumo(LocalDate.of(2026, 10, 26), TipoResumoDiario.HISTORICO)));

        mvc.perform(get("/api/resumos-diarios/historico").param("inicio", "2026-10-20").param("fim", "2026-10-26"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].data").value("2026-10-26"))
                .andExpect(jsonPath("$[0].tipo").value("HISTORICO"))
                .andExpect(jsonPath("$[0].descricaoClima").value("Chuva"));
    }

    @Test
    void limitaPrevisoes() throws Exception {
        mvc.perform(get("/api/resumos-diarios/previsoes").param("dias", "17"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").exists());
    }
}
