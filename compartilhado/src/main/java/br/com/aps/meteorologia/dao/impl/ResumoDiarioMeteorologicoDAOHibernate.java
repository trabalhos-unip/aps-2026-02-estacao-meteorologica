package br.com.aps.meteorologia.dao.impl;

import br.com.aps.meteorologia.dao.ResumoDiarioMeteorologicoDAO;
import br.com.aps.meteorologia.model.ResumoDiarioMeteorologico;
import br.com.aps.meteorologia.model.TipoResumoDiario;

import java.time.LocalDate;
import java.util.List;

/** Persistência idempotente dos resumos diários, sempre isolada por estação. */
public class ResumoDiarioMeteorologicoDAOHibernate implements ResumoDiarioMeteorologicoDAO {
    @Override
    public ResumoDiarioMeteorologico salvarOuAtualizar(ResumoDiarioMeteorologico resumo) {
        validar(resumo);
        return OperacaoHibernate.executarTransacao("Erro ao salvar o resumo diário", sessao -> {
            ResumoDiarioMeteorologico existente = sessao.createQuery(
                    "from ResumoDiarioMeteorologico r where r.estacaoId = :estacaoId and r.data = :data and r.tipo = :tipo",
                    ResumoDiarioMeteorologico.class).setParameter("estacaoId", resumo.getEstacaoId())
                    .setParameter("data", resumo.getData()).setParameter("tipo", resumo.getTipo()).uniqueResult();
            if (existente == null) { sessao.persist(resumo); return resumo; }
            existente.setTemperaturaMinima(resumo.getTemperaturaMinima());
            existente.setTemperaturaMaxima(resumo.getTemperaturaMaxima());
            existente.setTemperaturaMedia(resumo.getTemperaturaMedia());
            existente.setUmidadeMedia(resumo.getUmidadeMedia());
            existente.setPrecipitacaoTotal(resumo.getPrecipitacaoTotal());
            existente.setVentoMaximo(resumo.getVentoMaximo());
            existente.setCodigoClima(resumo.getCodigoClima());
            existente.setFonteDados(resumo.getFonteDados());
            existente.setAtualizadoEm(resumo.getAtualizadoEm());
            return existente;
        });
    }

    @Override
    public List<ResumoDiarioMeteorologico> buscarHistorico(Long estacaoId, LocalDate inicio, LocalDate fim) {
        return OperacaoHibernate.consultar("Erro ao consultar resumos diários", sessao -> sessao.createQuery(
                "from ResumoDiarioMeteorologico r where r.estacaoId = :estacaoId and r.tipo = :tipo and r.data between :inicio and :fim order by r.data",
                ResumoDiarioMeteorologico.class).setParameter("estacaoId", estacaoId)
                .setParameter("tipo", TipoResumoDiario.HISTORICO).setParameter("inicio", inicio).setParameter("fim", fim).getResultList());
    }

    @Override
    public List<ResumoDiarioMeteorologico> buscarPrevisoes(Long estacaoId, LocalDate aPartirDe, int limite) {
        if (limite < 1) throw new IllegalArgumentException("O limite deve ser maior que zero");
        return OperacaoHibernate.consultar("Erro ao consultar previsões", sessao -> sessao.createQuery(
                "from ResumoDiarioMeteorologico r where r.estacaoId = :estacaoId and r.tipo = :tipo and r.data >= :data order by r.data",
                ResumoDiarioMeteorologico.class).setParameter("estacaoId", estacaoId)
                .setParameter("tipo", TipoResumoDiario.PREVISAO).setParameter("data", aPartirDe).setMaxResults(limite).getResultList());
    }

    @Override
    public int removerPrevisoesAnterioresA(Long estacaoId, LocalDate data) {
        return OperacaoHibernate.executarTransacao("Erro ao limpar previsões antigas", sessao -> sessao
                .createMutationQuery("delete from ResumoDiarioMeteorologico r where r.estacaoId = :estacaoId and r.tipo = :tipo and r.data < :data")
                .setParameter("estacaoId", estacaoId).setParameter("tipo", TipoResumoDiario.PREVISAO).setParameter("data", data).executeUpdate());
    }

    private static void validar(ResumoDiarioMeteorologico resumo) {
        if (resumo == null || resumo.getEstacaoId() == null || resumo.getData() == null || resumo.getTipo() == null) {
            throw new IllegalArgumentException("Estação, data e tipo do resumo são obrigatórios");
        }
    }
}
