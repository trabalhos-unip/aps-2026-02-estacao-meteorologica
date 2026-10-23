package br.com.aps.meteorologia.dao.impl;

import br.com.aps.meteorologia.config.HibernateUtil;
import br.com.aps.meteorologia.dao.LeituraMeteorologicaDAO;
import br.com.aps.meteorologia.dao.PersistenciaException;
import br.com.aps.meteorologia.model.LeituraMeteorologica;
import br.com.aps.meteorologia.model.ModoOperacao;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.query.Query;

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
    public Optional<LeituraMeteorologica> buscarUltima(ModoOperacao modo) {
        Session sessao = null;
        try {
            sessao = HibernateUtil.getSessionFactory().openSession();
            return consulta(sessao, filtroModo("", modo) + " order by l.dataHoraColeta desc, l.id desc", modo)
                    .setMaxResults(1)
                    .uniqueResultOptional();
        } catch (RuntimeException e) {
            throw falha("Erro ao buscar a última leitura", e);
        } finally {
            fechar(sessao);
        }
    }

    @Override
    public List<LeituraMeteorologica> buscarUltimas(int limite, ModoOperacao modo) {
        validarLimite(limite);
        Session sessao = null;
        try {
            sessao = HibernateUtil.getSessionFactory().openSession();
            List<LeituraMeteorologica> maisRecentesPrimeiro = consulta(sessao,
                    filtroModo("", modo) + " order by l.dataHoraColeta desc, l.id desc", modo)
                    .setMaxResults(limite)
                    .getResultList();
            return emOrdemCronologica(maisRecentesPrimeiro);
        } catch (RuntimeException e) {
            throw falha("Erro ao buscar as últimas leituras", e);
        } finally {
            fechar(sessao);
        }
    }

    @Override
    public List<LeituraMeteorologica> buscarPorPeriodo(LocalDateTime dataInicio, LocalDateTime dataFim,
                                                       ModoOperacao modo, int limite) {
        if (dataInicio == null || dataFim == null) {
            throw new IllegalArgumentException("Data de início e data de fim são obrigatórias");
        }
        if (dataInicio.isAfter(dataFim)) {
            throw new IllegalArgumentException("A data de início deve ser anterior à data de fim");
        }
        validarLimite(limite);
        Session sessao = null;
        try {
            sessao = HibernateUtil.getSessionFactory().openSession();
            String hql = filtroModo(" where l.dataHoraColeta between :inicio and :fim", modo)
                    + " order by l.dataHoraColeta desc, l.id desc";
            List<LeituraMeteorologica> maisRecentesPrimeiro = consulta(sessao, hql, modo)
                    .setParameter("inicio", dataInicio)
                    .setParameter("fim", dataFim)
                    .setMaxResults(limite)
                    .getResultList();
            return emOrdemCronologica(maisRecentesPrimeiro);
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

    /** Acrescenta o filtro de modo à cláusula where, quando um modo foi informado. */
    private static String filtroModo(String where, ModoOperacao modo) {
        if (modo == null) {
            return where;
        }
        return where.isEmpty() ? " where l.modoOperacao = :modo" : where + " and l.modoOperacao = :modo";
    }

    /** Cria a consulta HQL sobre LeituraMeteorologica e preenche o parâmetro do modo, se houver. */
    private static Query<LeituraMeteorologica> consulta(Session sessao, String restoHql, ModoOperacao modo) {
        Query<LeituraMeteorologica> consulta =
                sessao.createQuery("from LeituraMeteorologica l" + restoHql, LeituraMeteorologica.class);
        if (modo != null) {
            consulta.setParameter("modo", modo);
        }
        return consulta;
    }

    private static void validarLimite(int limite) {
        if (limite <= 0) {
            throw new IllegalArgumentException("O limite deve ser maior que zero");
        }
    }

    /** As consultas buscam as mais recentes primeiro (para o limite); a página recebe em ordem cronológica. */
    private static List<LeituraMeteorologica> emOrdemCronologica(List<LeituraMeteorologica> maisRecentesPrimeiro) {
        List<LeituraMeteorologica> cronologica = new ArrayList<>(maisRecentesPrimeiro);
        Collections.reverse(cronologica);
        return cronologica;
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
