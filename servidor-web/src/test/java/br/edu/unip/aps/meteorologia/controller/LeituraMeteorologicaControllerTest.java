package br.edu.unip.aps.meteorologia.controller;

import br.edu.unip.aps.meteorologia.dao.LeituraMeteorologicaDAO;
import br.edu.unip.aps.meteorologia.model.LeituraMeteorologica;
import br.edu.unip.aps.meteorologia.service.ConsultaMeteorologicaService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Testa os endpoints com o DAO simulado (sem banco). */
@WebMvcTest(LeituraMeteorologicaController.class)
@Import(ConsultaMeteorologicaService.class)
class LeituraMeteorologicaControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private LeituraMeteorologicaDAO dao;

    private LeituraMeteorologica leitura(long id, double temperatura) {
        LeituraMeteorologica l = new LeituraMeteorologica();
        l.setId(id);
        l.setDataHoraColeta(LocalDateTime.of(2026, 10, 20, 22, 20, 5));
        l.setDataHoraFonte(LocalDateTime.of(2026, 10, 20, 22, 15));
        l.setTemperatura(temperatura);
        l.setUmidadeRelativa(89);
        l.setCodigoClima(3);
        l.setPresencaPrecipitacao(false);
        return l;
    }

    @Test
    void ultimaLeitura() throws Exception {
        when(dao.buscarUltima()).thenReturn(Optional.of(leitura(7, 14.0)));

        mvc.perform(get("/api/leituras/ultima"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(7))
                .andExpect(jsonPath("$.temperatura").value(14.0))
                .andExpect(jsonPath("$.descricaoClima").value("Nublado"))
                .andExpect(jsonPath("$.dataHoraColeta").value("2026-10-20T22:20:05"))
                .andExpect(jsonPath("$.precipitacao").isEmpty());
    }

    @Test
    void bancoVazioDevolveMensagem() throws Exception {
        when(dao.buscarUltima()).thenReturn(Optional.empty());

        mvc.perform(get("/api/leituras/ultima"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.erro").value("Nenhuma leitura registrada ainda"));
    }

    @Test
    void ultimasLeituras() throws Exception {
        when(dao.buscarUltimas(2)).thenReturn(List.of(leitura(1, 13.0), leitura(2, 14.0)));

        mvc.perform(get("/api/leituras").param("limite", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[1].temperatura").value(14.0));
    }

    @Test
    void limiteInvalido() throws Exception {
        mvc.perform(get("/api/leituras").param("limite", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").exists());
    }

    @Test
    void consultaPorPeriodo() throws Exception {
        when(dao.buscarPorPeriodo(any(), any())).thenReturn(List.of(leitura(1, 13.0)));

        mvc.perform(get("/api/leituras/periodo")
                        .param("inicio", "2026-10-20T00:00:00")
                        .param("fim", "2026-10-20T23:59:59"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void periodoComDataInvalida() throws Exception {
        mvc.perform(get("/api/leituras/periodo").param("inicio", "ontem").param("fim", "hoje"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void bancoIndisponivel() throws Exception {
        when(dao.buscarUltimas(100)).thenThrow(new DataAccessResourceFailureException("Connection refused"));

        mvc.perform(get("/api/leituras"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.erro").value("Banco de dados indisponível no momento"));
    }
}
