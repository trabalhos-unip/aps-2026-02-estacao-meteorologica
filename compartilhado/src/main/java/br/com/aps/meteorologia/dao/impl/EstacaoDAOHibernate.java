package br.com.aps.meteorologia.dao.impl;

import br.com.aps.meteorologia.dao.IEstacaoDAO;
import br.com.aps.meteorologia.model.Estacao;

import java.text.Normalizer;
import java.util.Locale;
import java.util.Optional;

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
            novaEstacao.setCidade(nomeNormalizado);
            novaEstacao.setEstado("SP");
            novaEstacao.setLatitude(latitude);
            novaEstacao.setLongitude(longitude);
            sessao.persist(novaEstacao);
            return novaEstacao;
        });
    }

    @Override
    public Estacao salvarEAtivar(String cidade, String estado, double latitude, double longitude, String fusoHorario) {
        validarNome(cidade); validarNome(estado); validarCoordenadas(latitude, longitude);
        String uf = estado.trim().toUpperCase(Locale.ROOT);
        if (!uf.matches("[A-Z]{2}")) throw new IllegalArgumentException("Informe a sigla do estado, por exemplo SP");
        String chave = "BR:" + uf + ":" + normalizar(cidade);
        return OperacaoHibernate.executarTransacao("Erro ao salvar a estação", sessao -> {
            sessao.createMutationQuery("update Estacao e set e.ativa = false where e.ativa = true").executeUpdate();
            Estacao estacao = sessao.createQuery("from Estacao e where e.nome = :nome", Estacao.class)
                    .setParameter("nome", chave).uniqueResult();
            boolean nova = estacao == null;
            if (nova) {
                estacao = new Estacao();
                estacao.setNome(chave);
            }
            estacao.setCidade(cidade.trim()); estacao.setEstado(uf); estacao.setPais("BR");
            estacao.setLatitude(latitude); estacao.setLongitude(longitude);
            estacao.setFusoHorario(fusoHorario == null || fusoHorario.isBlank() ? "America/Sao_Paulo" : fusoHorario);
            estacao.setAtiva(true);
            // Todos os campos NOT NULL são preenchidos antes do persist, pois a geração
            // de ID pode fazer o Hibernate validar a entidade imediatamente.
            if (nova) sessao.persist(estacao);
            return estacao;
        });
    }

    @Override
    public Optional<Estacao> buscarAtiva() {
        return Optional.ofNullable(OperacaoHibernate.consultar("Erro ao buscar a estação ativa", sessao ->
                sessao.createQuery("from Estacao e where e.ativa = true order by e.id desc", Estacao.class)
                        .setMaxResults(1).uniqueResult()));
    }

    @Override
    public Optional<Estacao> buscarPorCidadeEEstado(String cidade, String estado) {
        validarNome(cidade);
        validarNome(estado);
        String uf = estado.trim().toUpperCase(Locale.ROOT);
        if (!uf.matches("[A-Z]{2}")) return Optional.empty();
        String chave = "BR:" + uf + ":" + normalizar(cidade);
        return Optional.ofNullable(OperacaoHibernate.consultar("Erro ao buscar a estação",
                sessao -> sessao.createQuery("from Estacao e where e.nome = :nome", Estacao.class)
                        .setParameter("nome", chave).uniqueResult()));
    }

    private static String normalizar(String valor) {
        return Normalizer.normalize(valor, Normalizer.Form.NFD).replaceAll("\\p{M}", "")
                .replaceAll("[^A-Za-z0-9]+", "-").replaceAll("(^-|-$)", "").toUpperCase(Locale.ROOT);
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
