package br.com.aps.meteorologia.dao;

import br.com.aps.meteorologia.config.HibernateUtil;
import br.com.aps.meteorologia.dao.impl.LeituraMeteorologicaDAOHibernate;
import br.com.aps.meteorologia.dao.impl.EstacaoDAOHibernate;
import br.com.aps.meteorologia.model.Estacao;
import br.com.aps.meteorologia.model.LeituraMeteorologica;
import br.com.aps.meteorologia.model.ModoOperacao;
import org.hibernate.Session;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Testa o DAO real (HibernateUtil + hibernate.cfg.xml) trocando o MySQL por H2 em memória. */
class LeituraMeteorologicaDAOHibernateTest {

    private static final LocalDateTime BASE = LocalDateTime.of(2026, 10, 20, 14, 0, 0);
    private static final Map<String, String> BANCO_DE_TESTE = Map.of(
            "hibernate.connection.driver_class", "org.h2.Driver",
            "hibernate.connection.url", "jdbc:h2:mem:teste;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
            "hibernate.connection.username", "sa",
            "hibernate.connection.password", "",
            "hibernate.hbm2ddl.auto", "create-drop");

    private final ILeituraMeteorologicaDAO dao = new LeituraMeteorologicaDAOHibernate();
    private final IEstacaoDAO estacoes = new EstacaoDAOHibernate();

    @BeforeAll
    static void usarBancoDeTeste() {
        BANCO_DE_TESTE.forEach(System::setProperty);
    }

    @AfterAll
    static void encerrar() {
        HibernateUtil.fechar();
        BANCO_DE_TESTE.keySet().forEach(System::clearProperty);
    }

    @BeforeEach
    void limparTabela() {
        try (Session sessao = HibernateUtil.getSessionFactory().openSession()) {
            sessao.beginTransaction();
            sessao.createMutationQuery("delete from LeituraMeteorologica").executeUpdate();
            sessao.createMutationQuery("delete from Estacao").executeUpdate();
            sessao.getTransaction().commit();
        }
    }

    private LeituraMeteorologica leitura(LocalDateTime coleta, Double temperatura) {
        LeituraMeteorologica l = new LeituraMeteorologica();
        l.setEstacaoId(1L);
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

    private LeituraMeteorologica leitura(LocalDateTime coleta, Double temperatura, ModoOperacao modo) {
        LeituraMeteorologica l = leitura(coleta, temperatura);
        l.setModoOperacao(modo);
        return l;
    }

    @Test
    void salvaERecuperaLeitura() {
        LeituraMeteorologica salva = dao.salvar(leitura(BASE, 22.4));

        assertThat(salva.getId()).isNotNull();
        assertThat(dao.contarTodas()).isEqualTo(1);
        assertThat(dao.buscarUltima()).get()
                .extracting(LeituraMeteorologica::getTemperatura, LeituraMeteorologica::getFonteDados)
                .containsExactly(22.4, "Open-Meteo");
    }

    @Test
    void bancoVazioNaoQuebra() {
        assertThat(dao.buscarUltima()).isEmpty();
        assertThat(dao.buscarUltimas(10)).isEmpty();
        assertThat(dao.contarTodas()).isZero();
    }

    @Test
    void aceitaValoresAusentes() {
        LeituraMeteorologica semValores = new LeituraMeteorologica();
        semValores.setEstacaoId(1L);
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
    void gravaCoordenadasEModo() {
        LeituraMeteorologica l = leitura(BASE, 20.0, ModoOperacao.SIMULADO);
        l.setLatitude(-23.5149);
        l.setLongitude(-46.6105);

        dao.salvar(l);

        LeituraMeteorologica lida = dao.buscarUltima().orElseThrow();
        assertThat(lida.getLatitude()).isEqualTo(-23.5149);
        assertThat(lida.getLongitude()).isEqualTo(-46.6105);
        assertThat(lida.getModoOperacao()).isEqualTo(ModoOperacao.SIMULADO);
    }

    @Test
    void modoPadraoEReal() {
        dao.salvar(leitura(BASE, 20.0));

        assertThat(dao.buscarUltima().orElseThrow().getModoOperacao()).isEqualTo(ModoOperacao.REAL);
    }

    @Test
    void filtraPorModo() {
        dao.salvar(leitura(BASE, 1.0, ModoOperacao.REAL));
        dao.salvar(leitura(BASE.plusSeconds(10), 2.0, ModoOperacao.SIMULADO));
        dao.salvar(leitura(BASE.plusSeconds(20), 3.0, ModoOperacao.REAL));
        dao.salvar(leitura(BASE.plusSeconds(30), 4.0, ModoOperacao.SIMULADO));

        assertThat(dao.buscarUltimas(10, ModoOperacao.REAL))
                .extracting(LeituraMeteorologica::getTemperatura).containsExactly(1.0, 3.0);
        assertThat(dao.buscarUltimas(10, ModoOperacao.SIMULADO))
                .extracting(LeituraMeteorologica::getTemperatura).containsExactly(2.0, 4.0);
        assertThat(dao.buscarUltimas(10, null)).hasSize(4);
        assertThat(dao.buscarUltima(ModoOperacao.REAL)).get()
                .extracting(LeituraMeteorologica::getTemperatura).isEqualTo(3.0);
        assertThat(dao.buscarPorPeriodo(BASE, BASE.plusMinutes(1), ModoOperacao.SIMULADO, 100))
                .extracting(LeituraMeteorologica::getTemperatura).containsExactly(2.0, 4.0);
    }

    @Test
    void periodoRespeitaOLimiteDevolvendoAsMaisRecentes() {
        for (int i = 0; i < 5; i++) {
            dao.salvar(leitura(BASE.plusSeconds(i * 10L), (double) i));
        }

        List<LeituraMeteorologica> periodo = dao.buscarPorPeriodo(BASE, BASE.plusMinutes(1), null, 3);

        assertThat(periodo).extracting(LeituraMeteorologica::getTemperatura).containsExactly(2.0, 3.0, 4.0);
    }

    @Test
    void validaParametros() {
        assertThatThrownBy(() -> dao.salvar(null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> dao.buscarUltimas(0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> dao.buscarPorPeriodo(BASE, BASE.minusDays(1)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> dao.buscarPorPeriodo(BASE, BASE.plusDays(1), null, 0))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void criaEstacaoDaCidadeEAtivaSomenteANova() {
        Estacao campinas = estacoes.salvarEAtivar("Campinas", "SP", -22.90556, -47.06083, "America/Sao_Paulo");

        assertThat(campinas.getId()).isNotNull();
        assertThat(campinas.getCidade()).isEqualTo("Campinas");
        assertThat(campinas.getEstado()).isEqualTo("SP");
        assertThat(campinas.getPais()).isEqualTo("BR");
        assertThat(estacoes.buscarAtiva()).map(Estacao::getId).contains(campinas.getId());

        Estacao sorocaba = estacoes.salvarEAtivar("Sorocaba", "SP", -23.50167, -47.45806, "America/Sao_Paulo");

        assertThat(sorocaba.getId()).isNotEqualTo(campinas.getId());
        assertThat(estacoes.buscarAtiva()).map(Estacao::getId).contains(sorocaba.getId());
    }
}
