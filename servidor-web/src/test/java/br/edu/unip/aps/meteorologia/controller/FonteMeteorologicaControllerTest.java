package br.edu.unip.aps.meteorologia.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(FonteMeteorologicaController.class)
@TestPropertySource(properties = {
        "openmeteo.url=https://api.open-meteo.com/v1/forecast",
        "openmeteo.latitude=-23.5015",
        "openmeteo.longitude=-47.4526",
        "openmeteo.timezone=America/Sao_Paulo",
        "estacao.cidade=Sorocaba"
})
class FonteMeteorologicaControllerTest {

    @Autowired
    private MockMvc mvc;

    @Test
    void informaLinkECoordenadasDaConsultaOficial() throws Exception {
        mvc.perform(get("/api/fonte"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Open-Meteo"))
                .andExpect(jsonPath("$.latitude").value(-23.5015))
                .andExpect(jsonPath("$.longitude").value(-47.4526))
                .andExpect(jsonPath("$.cidade").value("Sorocaba"))
                .andExpect(jsonPath("$.urlConsulta").value(org.hamcrest.Matchers.containsString("latitude=-23.5015")));
    }
}
