package br.com.aps.meteorologia.app;

import br.com.aps.meteorologia.client.ApiMeteorologicaClient;
import br.com.aps.meteorologia.dao.LeituraMeteorologicaDAO;
import br.com.aps.meteorologia.service.ColetaMeteorologicaService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.assertj.core.api.Assertions.assertThat;

/** Sobe o Spring Boot do coletor e confere se os objetos foram criados e ligados (sem API real e sem banco). */
@SpringBootTest
class CollectorApplicationTest {

    @MockitoBean
    private ApiMeteorologicaClient client;

    @MockitoBean
    private LeituraMeteorologicaDAO dao;

    @Autowired
    private ApplicationContext contexto;

    @Test
    void sobeComTodosOsComponentes() {
        assertThat(contexto.getBean(ColetaMeteorologicaService.class)).isNotNull();
        assertThat(contexto.getBean(LeituraMeteorologicaDAO.class)).isSameAs(dao);
    }
}
