package br.edu.unip.aps.meteorologia.dao.impl;

import br.edu.unip.aps.meteorologia.dao.ResumoDiarioMeteorologicoDAO;
import br.edu.unip.aps.meteorologia.model.ResumoDiarioMeteorologico;
import br.edu.unip.aps.meteorologia.model.TipoResumoDiario;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.hibernate.Session;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/** Implementação Hibernate com atualização idempotente por data e tipo. */
@Repository
@Transactional(readOnly = true)
public class ResumoDiarioMeteorologicoDAOImpl implements ResumoDiarioMeteorologicoDAO {

    @PersistenceContext
    private EntityManager entityManager;

    private Session sessao() { return entityManager.unwrap(Session.class); }

    @Override
    @Transactional
    public ResumoDiarioMeteorologico salvarOuAtualizar(ResumoDiarioMeteorologico resumo) {
        if (resumo == null || resumo.getData() == null || resumo.getTipo() == null) {
            throw new IllegalArgumentException("Resumo, data e tipo são obrigatórios");
        }
        ResumoDiarioMeteorologico existente = sessao().createQuery(
                        "from ResumoDiarioMeteorologico r where r.data = :data and r.tipo = :tipo",
                        ResumoDiarioMeteorologico.class)
                .setParameter("data", resumo.getData())
                .setParameter("tipo", resumo.getTipo())
                .uniqueResult();
        if (existente == null) {
            sessao().persist(resumo);
            return resumo;
        }
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
    }

    @Override
    public List<ResumoDiarioMeteorologico> buscarHistorico(LocalDate inicio, LocalDate fim) {
        return sessao().createQuery("from ResumoDiarioMeteorologico r where r.tipo = :tipo and r.data between :inicio and :fim order by r.data",
                        ResumoDiarioMeteorologico.class)
                .setParameter("tipo", TipoResumoDiario.HISTORICO)
                .setParameter("inicio", inicio).setParameter("fim", fim).getResultList();
    }

    @Override
    public List<ResumoDiarioMeteorologico> buscarPrevisoes(LocalDate aPartirDe, int limite) {
        if (limite < 1) throw new IllegalArgumentException("O limite deve ser maior que zero");
        return sessao().createQuery("from ResumoDiarioMeteorologico r where r.tipo = :tipo and r.data >= :data order by r.data",
                        ResumoDiarioMeteorologico.class)
                .setParameter("tipo", TipoResumoDiario.PREVISAO).setParameter("data", aPartirDe)
                .setMaxResults(limite).getResultList();
    }

    @Override
    @Transactional
    public int removerPrevisoesAnterioresA(LocalDate data) {
        return sessao().createMutationQuery("delete from ResumoDiarioMeteorologico r where r.tipo = :tipo and r.data < :data")
                .setParameter("tipo", TipoResumoDiario.PREVISAO).setParameter("data", data).executeUpdate();
    }
}
