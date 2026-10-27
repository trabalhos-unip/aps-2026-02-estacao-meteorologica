package br.com.aps.meteorologia.dao.impl;

import br.com.aps.meteorologia.dao.IEstacaoDAO;
import br.com.aps.meteorologia.model.Estacao;

/** Persistência de estações implementada com Hibernate. */
public class EstacaoDAOHibernate implements IEstacaoDAO {

    @Override
    public Estacao buscarOuCriar(String nome, double latitude, double longitude) {
        validarNome(nome);
        validarCoordenadas(latitude, longitude);
        String nomeNormalizado = nome.trim();

        Estacao existente = OperacaoHibernate.consultar("Erro ao buscar a estação",
                sessao -> sessao.createQuery("from Estacao e where e.nome = :nome", Estacao.class)
                        .setParameter("nome", nomeNormalizado)
                        .uniqueResult());
        if (existente != null) {
            return existente;
        }

        return OperacaoHibernate.executarTransacao("Erro ao criar a estação", sessao -> {
            Estacao novaEstacao = new Estacao();
            novaEstacao.setNome(nomeNormalizado);
            novaEstacao.setLatitude(latitude);
            novaEstacao.setLongitude(longitude);
            sessao.persist(novaEstacao);
            return novaEstacao;
        });
    }

    private static void validarNome(String nome) {
        if (nome == null || nome.isBlank()) {
            throw new IllegalArgumentException("O nome da estação é obrigatório");
        }
    }

    private static void validarCoordenadas(double latitude, double longitude) {
        if (latitude < -90 || latitude > 90 || longitude < -180 || longitude > 180) {
            throw new IllegalArgumentException("As coordenadas da estação são inválidas");
        }
    }
}
