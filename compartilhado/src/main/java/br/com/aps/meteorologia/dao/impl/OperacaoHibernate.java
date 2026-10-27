package br.com.aps.meteorologia.dao.impl;

import br.com.aps.meteorologia.config.HibernateUtil;
import br.com.aps.meteorologia.dao.PersistenciaException;
import org.hibernate.Session;
import org.hibernate.Transaction;

import java.util.function.Function;

/** Centraliza o ciclo de vida de sessões e transações usado pelos DAOs Hibernate. */
final class OperacaoHibernate {

    private OperacaoHibernate() {
    }

    static <T> T consultar(String mensagemErro, Function<Session, T> consulta) {
        try (Session sessao = HibernateUtil.getSessionFactory().openSession()) {
            return consulta.apply(sessao);
        } catch (RuntimeException erro) {
            throw converterErro(mensagemErro, erro);
        }
    }

    static <T> T executarTransacao(String mensagemErro, Function<Session, T> operacao) {
        Session sessao = null;
        Transaction transacao = null;
        try {
            sessao = HibernateUtil.getSessionFactory().openSession();
            transacao = sessao.beginTransaction();
            T resultado = operacao.apply(sessao);
            transacao.commit();
            return resultado;
        } catch (RuntimeException erro) {
            desfazerSemOcultarErroOriginal(transacao);
            throw converterErro(mensagemErro, erro);
        } finally {
            fecharSessao(sessao);
        }
    }

    private static void fecharSessao(Session sessao) {
        if (sessao != null && sessao.isOpen()) {
            sessao.close();
        }
    }

    private static void desfazerSemOcultarErroOriginal(Transaction transacao) {
        try {
            if (transacao != null && transacao.isActive()) {
                transacao.rollback();
            }
        } catch (RuntimeException ignorada) {
            // A falha original da operação deve ser preservada.
        }
    }

    private static PersistenciaException converterErro(String mensagem, RuntimeException erro) {
        if (erro instanceof PersistenciaException persistenciaException) {
            return persistenciaException;
        }
        return new PersistenciaException(mensagem + ": " + erro.getMessage(), erro);
    }
}
