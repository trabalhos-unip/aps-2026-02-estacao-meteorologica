package br.edu.unip.aps.meteorologia.dao;

import br.edu.unip.aps.meteorologia.dao.impl.ResumoDiarioMeteorologicoDAOImpl;
import br.edu.unip.aps.meteorologia.model.ResumoDiarioMeteorologico;
import br.edu.unip.aps.meteorologia.model.TipoResumoDiario;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(ResumoDiarioMeteorologicoDAOImpl.class)
class ResumoDiarioMeteorologicoDAOImplTest {

    @Autowired
    private ResumoDiarioMeteorologicoDAO dao;

    private ResumoDiarioMeteorologico resumo(LocalDate data, TipoResumoDiario tipo, double temperatura) {
        ResumoDiarioMeteorologico resumo = new ResumoDiarioMeteorologico();
        resumo.setData(data);
        resumo.setTipo(tipo);
        resumo.setTemperaturaMinima(temperatura - 2);
        resumo.setTemperaturaMaxima(temperatura + 2);
        resumo.setTemperaturaMedia(temperatura);
        resumo.setUmidadeMedia(70.0);
        resumo.setPrecipitacaoTotal(1.2);
        resumo.setVentoMaximo(15.0);
        resumo.setCodigoClima(3);
        resumo.setFonteDados("Open-Meteo (teste)");
        resumo.setAtualizadoEm(LocalDateTime.of(2026, 10, 27, 12, 0));
        return resumo;
    }

    @Test
    void atualizaOMesmoDiaESegregaHistoricoDePrevisao() {
        LocalDate ontem = LocalDate.of(2026, 10, 26);
        LocalDate hoje = LocalDate.of(2026, 10, 27);
        dao.salvarOuAtualizar(resumo(ontem, TipoResumoDiario.HISTORICO, 21));
        dao.salvarOuAtualizar(resumo(hoje, TipoResumoDiario.PREVISAO, 24));
        ResumoDiarioMeteorologico atualizado = dao.salvarOuAtualizar(resumo(ontem, TipoResumoDiario.HISTORICO, 23));

        assertThat(dao.buscarHistorico(ontem.minusDays(1), hoje))
                .singleElement().extracting(ResumoDiarioMeteorologico::getTemperaturaMedia).isEqualTo(23.0);
        assertThat(dao.buscarPrevisoes(hoje, 5)).singleElement()
                .extracting(ResumoDiarioMeteorologico::getTipo).isEqualTo(TipoResumoDiario.PREVISAO);
        assertThat(atualizado.getId()).isNotNull();
    }
}
