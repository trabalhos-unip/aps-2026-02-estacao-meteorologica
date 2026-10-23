package br.com.aps.meteorologia.dao.impl;

import br.com.aps.meteorologia.config.HibernateUtil;
import br.com.aps.meteorologia.dao.LeituraMeteorologicaDAO;
import br.com.aps.meteorologia.dao.PersistenciaException;
import br.com.aps.meteorologia.model.LeituraMeteorologica;
import org.hibernate.Session;
import org.hibernate.Transaction;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Implementação do DAO com Hibernate. Toda operação de banco do sistema fica concentrada aqui.
 *
 * <p>Cada operação abre uma sessão pela {@link HibernateUtil}, executa e fecha a sessão no {@code finally}.
 * A gravação usa transação: {@code commit} em caso de sucesso e {@code rollback} em caso de erro.
 * Qualquer falha é devolvida como {@link PersistenciaException}.
 */
public class LeituraMeteorologicaDAOImpl implements LeituraMeteorologicaDAO {

    @Override
    public LeituraMeteorologica salvar(LeituraMeteorologica leitura) {
        if (leitura == null) {
            throw new IllegalArgumentException("A leitura não pode ser nula");
        }
        Session sessao = null;
        Transaction transacao = null;
        try {
            sessao = HibernateUtil.getSessionFactory().openSession();
            transacao = sessao.beginTransaction();
            sessao.persist(leitura);
            transacao.commit();
            return leitura;
        } catch (RuntimeException e) {
            desfazer(transacao);
            throw falha("Erro ao salvar a leitura", e);
        } finally {
            fechar(sessao);
        }
    }

    @Override
    public Optional<LeituraMeteorologica> buscarUltima() {
        Session sessao = null;
        try {
            sessao = HibernateUtil.getSessionFactory().openSession();
            return sessao
                    .createQuery("from LeituraMeteorologica l order by l.dataHoraColeta desc, l.id desc",
                            LeituraMeteorologica.class)
                    .setMaxResults(1)
                    .uniqueResultOptional();
        } catch (RuntimeException e) {
            throw falha("Erro ao buscar a última leitura", e);
        } finally {
            fechar(sessao);
        }
    }

    @Override
    public List<LeituraMeteorologica> buscarUltimas(int limite) {
        if (limite <= 0) {
            throw new IllegalArgumentException("O limite deve ser maior que zero");
        }
        Session sessao = null;
        try {
            sessao = HibernateUtil.getSessionFactory().openSession();
            List<LeituraMeteorologica> maisRecentesPrimeiro = sessao
                    .createQuery("from LeituraMeteorologica l order by l.dataHoraColeta desc, l.id desc",
                            LeituraMeteorologica.class)
                    .setMaxResults(limite)
                    .getResultList();
            List<LeituraMeteorologica> cronologica = new ArrayList<>(maisRecentesPrimeiro);
            Collections.reverse(cronologica);
            return cronologica;
        } catch (RuntimeException e) {
            throw falha("Erro ao buscar as últimas leituras", e);
        } finally {
            fechar(sessao);
        }
    }

    @Override
    public List<LeituraMeteorologica> buscarPorPeriodo(LocalDateTime dataInicio, LocalDateTime dataFim) {
        if (dataInicio == null || dataFim == null) {
            throw new IllegalArgumentException("Data de início e data de fim são obrigatórias");
        }
        if (dataInicio.isAfter(dataFim)) {
            throw new IllegalArgumentException("A data de início deve ser anterior à data de fim");
        }
        Session sessao = null;
        try {
            sessao = HibernateUtil.getSessionFactory().openSession();
            return sessao
                    .createQuery("from LeituraMeteorologica l"
                                    + " where l.dataHoraColeta between :inicio and :fim"
                                    + " order by l.dataHoraColeta asc, l.id asc",
                            LeituraMeteorologica.class)
                    .setParameter("inicio", dataInicio)
                    .setParameter("fim", dataFim)
                    .getResultList();
        } catch (RuntimeException e) {
            throw falha("Erro ao buscar leituras por período", e);
        } finally {
            fechar(sessao);
        }
    }

    @Override
    public long contar() {
        Session sessao = null;
        try {
            sessao = HibernateUtil.getSessionFactory().openSession();
            return sessao.createQuery("select count(l) from LeituraMeteorologica l", Long.class)
                    .getSingleResult();
        } catch (RuntimeException e) {
            throw falha("Erro ao contar as leituras", e);
        } finally {
            fechar(sessao);
        }
    }

    private static void desfazer(Transaction transacao) {
        try {
            if (transacao != null && transacao.isActive()) {
                transacao.rollback();
            }
        } catch (RuntimeException ignorada) {
            // a falha original é mais importante que a falha no rollback
        }
    }

    private static void fechar(Session sessao) {
        if (sessao != null && sessao.isOpen()) {
            sessao.close();
        }
    }

    private static PersistenciaException falha(String mensagem, RuntimeException e) {
        if (e instanceof PersistenciaException persistencia) {
            return persistencia;
        }
        return new PersistenciaException(mensagem + ": " + e.getMessage(), e);
    }
}
