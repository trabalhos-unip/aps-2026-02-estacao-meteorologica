package br.com.aps.meteorologia.dao;

import br.com.aps.meteorologia.model.LogColeta;

/** Contrato de persistência do histórico de tentativas de coleta. */
public interface ILogColetaDAO {

    LogColeta salvar(LogColeta logColeta);
}
