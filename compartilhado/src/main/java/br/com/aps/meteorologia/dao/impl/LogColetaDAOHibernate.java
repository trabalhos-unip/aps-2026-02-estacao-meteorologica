package br.com.aps.meteorologia.dao.impl;

import br.com.aps.meteorologia.dao.ILogColetaDAO;
import br.com.aps.meteorologia.model.LogColeta;

/** Persistência do histórico de coletas implementada com Hibernate. */
public class LogColetaDAOHibernate implements ILogColetaDAO {

    @Override
    public LogColeta salvar(LogColeta logColeta) {
        if (logColeta == null) {
            throw new IllegalArgumentException("O log de coleta não pode ser nulo");
        }
        return OperacaoHibernate.executarTransacao("Erro ao salvar o log de coleta", sessao -> {
            sessao.persist(logColeta);
            return logColeta;
        });
    }
}
