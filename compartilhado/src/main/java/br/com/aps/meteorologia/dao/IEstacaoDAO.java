package br.com.aps.meteorologia.dao;

import br.com.aps.meteorologia.model.Estacao;

/** Contrato de persistência para o cadastro de estações meteorológicas. */
public interface IEstacaoDAO {

    Estacao buscarOuCriar(String nome, double latitude, double longitude);
}
