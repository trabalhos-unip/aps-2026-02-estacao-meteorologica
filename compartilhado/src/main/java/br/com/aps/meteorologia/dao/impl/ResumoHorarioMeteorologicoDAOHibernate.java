package br.com.aps.meteorologia.dao.impl;

import br.com.aps.meteorologia.dao.ResumoHorarioMeteorologicoDAO;
import br.com.aps.meteorologia.model.ResumoHorarioMeteorologico;
import br.com.aps.meteorologia.model.TipoResumoDiario;

import java.time.LocalDateTime;
import java.util.List;

/** Persistência idempotente da série horária da Open-Meteo, isolada por estação. */
public class ResumoHorarioMeteorologicoDAOHibernate implements ResumoHorarioMeteorologicoDAO {
    @Override
    public ResumoHorarioMeteorologico salvarOuAtualizar(ResumoHorarioMeteorologico resumo) {
        if (resumo == null || resumo.getEstacaoId() == null || resumo.getDataHora() == null || resumo.getTipo() == null) {
            throw new IllegalArgumentException("Estação, data/hora e tipo do resumo são obrigatórios");
        }
        return OperacaoHibernate.executarTransacao("Erro ao salvar o resumo horário", sessao -> {
            ResumoHorarioMeteorologico existente = sessao.createQuery(
                    "from ResumoHorarioMeteorologico r where r.estacaoId = :estacaoId and r.dataHora = :dataHora and r.tipo = :tipo",
                    ResumoHorarioMeteorologico.class).setParameter("estacaoId", resumo.getEstacaoId())
                    .setParameter("dataHora", resumo.getDataHora()).setParameter("tipo", resumo.getTipo()).uniqueResult();
            if (existente == null) { sessao.persist(resumo); return resumo; }
            existente.setTemperatura(resumo.getTemperatura());
            existente.setUmidadeRelativa(resumo.getUmidadeRelativa());
            existente.setPrecipitacao(resumo.getPrecipitacao());
            existente.setChuva(resumo.getChuva());
            existente.setVelocidadeVento(resumo.getVelocidadeVento());
            existente.setCodigoClima(resumo.getCodigoClima());
            existente.setFonteDados(resumo.getFonteDados());
            existente.setAtualizadoEm(resumo.getAtualizadoEm());
            return existente;
        });
    }

    @Override
    public List<ResumoHorarioMeteorologico> buscarHistorico(Long estacaoId, LocalDateTime inicio, LocalDateTime fim) {
        return OperacaoHibernate.consultar("Erro ao consultar resumos horários", sessao -> sessao.createQuery(
                "from ResumoHorarioMeteorologico r where r.estacaoId = :estacaoId and r.tipo = :tipo and r.dataHora between :inicio and :fim order by r.dataHora",
                ResumoHorarioMeteorologico.class).setParameter("estacaoId", estacaoId)
                .setParameter("tipo", TipoResumoDiario.HISTORICO).setParameter("inicio", inicio).setParameter("fim", fim).getResultList());
    }

    @Override
    public int removerPrevisoesAnterioresA(Long estacaoId, LocalDateTime dataHora) {
        return OperacaoHibernate.executarTransacao("Erro ao limpar previsões horárias antigas", sessao -> sessao
                .createMutationQuery("delete from ResumoHorarioMeteorologico r where r.estacaoId = :estacaoId and r.tipo = :tipo and r.dataHora < :dataHora")
                .setParameter("estacaoId", estacaoId).setParameter("tipo", TipoResumoDiario.PREVISAO).setParameter("dataHora", dataHora).executeUpdate());
    }
}
