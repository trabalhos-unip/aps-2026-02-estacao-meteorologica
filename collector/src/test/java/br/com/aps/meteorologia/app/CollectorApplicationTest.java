package br.com.aps.meteorologia.app;

import br.com.aps.meteorologia.client.ClienteOpenMeteo;
import br.com.aps.meteorologia.client.IFonteMeteorologica;
import br.com.aps.meteorologia.client.SimuladorMeteorologico;
import br.com.aps.meteorologia.dao.ILeituraMeteorologicaDAO;
import br.com.aps.meteorologia.service.ColetaMeteorologicaService;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.assertj.core.api.Assertions.assertThat;

/** Sobe o Spring Boot do coletor e confere se os objetos foram criados e ligados (sem API real e sem banco). */
class CollectorApplicationTest {

    @Nested
    @SpringBootTest(classes = CollectorApplication.class)
    class ModoReal {

        @MockitoBean
        private ColetaMeteorologicaService coletaMeteorologicaService;

        @MockitoBean
        private ClienteOpenMeteo clienteOpenMeteo;

        @MockitoBean
        private ILeituraMeteorologicaDAO dao;

        @Autowired
        private ApplicationContext contexto;

        @Test
        void usaAApiPorPadrao() {
            assertThat(contexto.getBean(ColetaMeteorologicaService.class)).isNotNull();
            assertThat(contexto.getBean(ILeituraMeteorologicaDAO.class)).isSameAs(dao);
            assertThat(contexto.getBeansOfType(IFonteMeteorologica.class)).hasSize(1);
            assertThat(contexto.getBeansOfType(SimuladorMeteorologico.class)).isEmpty();
        }
    }

    @Nested
    @SpringBootTest(classes = CollectorApplication.class,
            properties = {"coleta.modo=SIMULADO", "coleta.cenario=TEMPO_SECO"})
    class ModoSimulado {

        @MockitoBean
        private ColetaMeteorologicaService coletaMeteorologicaService;

        @MockitoBean
        private ILeituraMeteorologicaDAO dao;

        @Autowired
        private ApplicationContext contexto;

        @Test
        void usaOSimulador() {
            assertThat(contexto.getBean(IFonteMeteorologica.class)).isInstanceOf(SimuladorMeteorologico.class);
            assertThat(contexto.getBeansOfType(ClienteOpenMeteo.class)).isEmpty();
        }
    }
}
