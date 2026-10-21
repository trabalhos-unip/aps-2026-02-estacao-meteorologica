package br.edu.unip.aps.meteorologia.dao;

import br.edu.unip.aps.meteorologia.dao.impl.LeituraMeteorologicaDAOImpl;
import br.edu.unip.aps.meteorologia.model.LeituraMeteorologica;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@Import(LeituraMeteorologicaDAOImpl.class)
class LeituraMeteorologicaDAOImplTest {

    private static final LocalDateTime BASE = LocalDateTime.of(2026, 10, 20, 14, 0, 0);

    @Autowired
    private LeituraMeteorologicaDAO dao;

    private LeituraMeteorologica leitura(LocalDateTime coleta, Double temperatura) {
        LeituraMeteorologica l = new LeituraMeteorologica();
        l.setDataHoraColeta(coleta);
        l.setDataHoraFonte(coleta.withMinute(0));
        l.setTemperatura(temperatura);
        l.setUmidadeRelativa(80);
        l.setPrecipitacao(0.0);
        l.setChuva(0.0);
        l.setVelocidadeVento(10.5);
        l.setCodigoClima(3);
        l.setPresencaPrecipitacao(false);
        return l;
    }

    @Test
    void salvaERecuperaLeitura() {
        LeituraMeteorologica salva = dao.salvar(leitura(BASE, 22.4));

        assertThat(salva.getId()).isNotNull();
        assertThat(dao.contar()).isEqualTo(1);
        assertThat(dao.buscarUltima()).get()
                .extracting(LeituraMeteorologica::getTemperatura, LeituraMeteorologica::getFonteDados)
                .containsExactly(22.4, "Open-Meteo");
    }

    @Test
    void bancoVazioNaoQuebra() {
        assertThat(dao.buscarUltima()).isEmpty();
        assertThat(dao.buscarUltimas(10)).isEmpty();
        assertThat(dao.contar()).isZero();
    }

    @Test
    void aceitaValoresAusentes() {
        LeituraMeteorologica semValores = new LeituraMeteorologica();
        semValores.setDataHoraColeta(BASE);

        dao.salvar(semValores);

        LeituraMeteorologica lida = dao.buscarUltima().orElseThrow();
        assertThat(lida.getTemperatura()).isNull();
        assertThat(lida.getUmidadeRelativa()).isNull();
    }

    @Test
    void buscarUltimasDevolveAsMaisRecentesEmOrdemCronologica() {
        // Gravadas fora de ordem de propósito
        dao.salvar(leitura(BASE.plusSeconds(20), 3.0));
        dao.salvar(leitura(BASE, 1.0));
        dao.salvar(leitura(BASE.plusSeconds(30), 4.0));
        dao.salvar(leitura(BASE.plusSeconds(10), 2.0));

        List<LeituraMeteorologica> ultimas = dao.buscarUltimas(3);

        assertThat(ultimas).extracting(LeituraMeteorologica::getTemperatura).containsExactly(2.0, 3.0, 4.0);
        assertThat(dao.buscarUltima()).get().extracting(LeituraMeteorologica::getTemperatura).isEqualTo(4.0);
    }

    @Test
    void buscarPorPeriodoFiltraEOrdena() {
        dao.salvar(leitura(BASE.minusMinutes(5), 0.0));
        dao.salvar(leitura(BASE.plusMinutes(2), 2.0));
        dao.salvar(leitura(BASE, 1.0));
        dao.salvar(leitura(BASE.plusMinutes(10), 9.0));

        List<LeituraMeteorologica> periodo = dao.buscarPorPeriodo(BASE, BASE.plusMinutes(5));

        assertThat(periodo).extracting(LeituraMeteorologica::getTemperatura).containsExactly(1.0, 2.0);
    }

    @Test
    void validaParametros() {
        assertThatThrownBy(() -> dao.salvar(null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> dao.buscarUltimas(0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> dao.buscarPorPeriodo(BASE, BASE.minusDays(1)))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
