package br.com.aps.meteorologia.dao.impl;

import br.com.aps.meteorologia.dao.ILeituraMeteorologicaDAO;
import br.com.aps.meteorologia.model.LeituraMeteorologica;
import br.com.aps.meteorologia.model.ModoOperacao;
import org.hibernate.Session;
import org.hibernate.query.Query;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/** Persistência das leituras meteorológicas implementada com Hibernate. */
public class LeituraMeteorologicaDAOHibernate implements ILeituraMeteorologicaDAO {

    @Override
    public LeituraMeteorologica salvar(LeituraMeteorologica leitura) {
        if (leitura == null) {
            throw new IllegalArgumentException("A leitura não pode ser nula");
        }
        return OperacaoHibernate.executarTransacao("Erro ao salvar a leitura", sessao -> {
            sessao.persist(leitura);
            return leitura;
        });
    }

    @Override
    public Optional<LeituraMeteorologica> buscarUltima(ModoOperacao modoOperacao) {
        return OperacaoHibernate.consultar("Erro ao buscar a última leitura", sessao ->
                criarConsulta(sessao, montarComplementoOrdenado("", modoOperacao), modoOperacao)
                        .setMaxResults(1)
                        .uniqueResultOptional());
    }

    @Override
    public List<LeituraMeteorologica> buscarUltimas(int limite, ModoOperacao modoOperacao) {
        validarLimite(limite);
        return OperacaoHibernate.consultar("Erro ao buscar as últimas leituras", sessao -> {
            List<LeituraMeteorologica> leiturasEmOrdemDecrescente = criarConsulta(
                    sessao, montarComplementoOrdenado("", modoOperacao), modoOperacao)
                    .setMaxResults(limite)
                    .getResultList();
            return inverterParaOrdemCronologica(leiturasEmOrdemDecrescente);
        });
    }

    @Override
    public List<LeituraMeteorologica> buscarPorPeriodo(LocalDateTime dataInicio, LocalDateTime dataFim,
                                                       ModoOperacao modoOperacao, int limite) {
        validarPeriodo(dataInicio, dataFim);
        validarLimite(limite);
        return OperacaoHibernate.consultar("Erro ao buscar leituras por período", sessao -> {
            String filtroPeriodo = " where l.dataHoraColeta between :inicio and :fim";
            List<LeituraMeteorologica> leiturasEmOrdemDecrescente = criarConsulta(
                    sessao, montarComplementoOrdenado(filtroPeriodo, modoOperacao), modoOperacao)
                    .setParameter("inicio", dataInicio)
                    .setParameter("fim", dataFim)
                    .setMaxResults(limite)
                    .getResultList();
            return inverterParaOrdemCronologica(leiturasEmOrdemDecrescente);
        });
    }

    @Override
    public long contarTodas() {
        return OperacaoHibernate.consultar("Erro ao contar as leituras", sessao ->
                sessao.createQuery("select count(l) from LeituraMeteorologica l", Long.class)
                        .getSingleResult());
    }

    private static String montarComplementoOrdenado(String clausulaWhere, ModoOperacao modoOperacao) {
        String ordenacao = " order by l.dataHoraColeta desc, l.id desc";
        if (modoOperacao == null) {
            return clausulaWhere + ordenacao;
        }
        String filtroModo = clausulaWhere.isEmpty()
                ? " where l.modoOperacao = :modoOperacao"
                : clausulaWhere + " and l.modoOperacao = :modoOperacao";
        return filtroModo + ordenacao;
    }

    private static Query<LeituraMeteorologica> criarConsulta(Session sessao, String complementoHql,
                                                              ModoOperacao modoOperacao) {
        Query<LeituraMeteorologica> consulta = sessao.createQuery(
                "from LeituraMeteorologica l" + complementoHql, LeituraMeteorologica.class);
        if (modoOperacao != null) {
            consulta.setParameter("modoOperacao", modoOperacao);
        }
        return consulta;
    }

    private static void validarPeriodo(LocalDateTime dataInicio, LocalDateTime dataFim) {
        if (dataInicio == null || dataFim == null) {
            throw new IllegalArgumentException("Data de início e data de fim são obrigatórias");
        }
        if (dataInicio.isAfter(dataFim)) {
            throw new IllegalArgumentException("A data de início deve ser anterior à data de fim");
        }
    }

    private static void validarLimite(int limite) {
        if (limite <= 0) {
            throw new IllegalArgumentException("O limite deve ser maior que zero");
        }
    }

    private static List<LeituraMeteorologica> inverterParaOrdemCronologica(
            List<LeituraMeteorologica> leiturasEmOrdemDecrescente) {
        List<LeituraMeteorologica> leiturasEmOrdemCronologica = new ArrayList<>(leiturasEmOrdemDecrescente);
        Collections.reverse(leiturasEmOrdemCronologica);
        return leiturasEmOrdemCronologica;
    }
}
