package br.com.aps.meteorologia.dao;

import br.com.aps.meteorologia.model.Estacao;

import java.util.Optional;

/** Contrato de persistência para o cadastro de estações meteorológicas. */
public interface IEstacaoDAO {

    Estacao buscarOuCriar(String nome, double latitude, double longitude);

    Estacao salvarEAtivar(String cidade, String estado, double latitude, double longitude, String fusoHorario);

    Optional<Estacao> buscarAtiva();
}
