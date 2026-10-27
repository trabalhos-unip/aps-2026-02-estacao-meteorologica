package br.edu.unip.aps.meteorologia.dao.impl;

import br.edu.unip.aps.meteorologia.dao.ResumoHorarioMeteorologicoDAO;
import br.edu.unip.aps.meteorologia.model.ResumoHorarioMeteorologico;
import br.edu.unip.aps.meteorologia.model.TipoResumoDiario;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.hibernate.Session;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Repository
@Transactional(readOnly = true)
public class ResumoHorarioMeteorologicoDAOImpl implements ResumoHorarioMeteorologicoDAO {
    @PersistenceContext private EntityManager entityManager;
    private Session sessao() { return entityManager.unwrap(Session.class); }

    @Override @Transactional
    public ResumoHorarioMeteorologico salvarOuAtualizar(ResumoHorarioMeteorologico resumo) {
        if (resumo == null || resumo.getDataHora() == null || resumo.getTipo() == null) throw new IllegalArgumentException("Resumo horário, data/hora e tipo são obrigatórios");
        ResumoHorarioMeteorologico existente = sessao().createQuery("from ResumoHorarioMeteorologico r where r.dataHora = :dataHora and r.tipo = :tipo", ResumoHorarioMeteorologico.class)
                .setParameter("dataHora", resumo.getDataHora()).setParameter("tipo", resumo.getTipo()).uniqueResult();
        if (existente == null) { sessao().persist(resumo); return resumo; }
        existente.setTemperatura(resumo.getTemperatura());
        existente.setUmidadeRelativa(resumo.getUmidadeRelativa());
        existente.setPrecipitacao(resumo.getPrecipitacao());
        existente.setChuva(resumo.getChuva());
        existente.setVelocidadeVento(resumo.getVelocidadeVento());
        existente.setCodigoClima(resumo.getCodigoClima());
        existente.setFonteDados(resumo.getFonteDados());
        existente.setAtualizadoEm(resumo.getAtualizadoEm());
        return existente;
    }

    @Override
    public List<ResumoHorarioMeteorologico> buscarHistorico(LocalDateTime inicio, LocalDateTime fim) {
        return sessao().createQuery("from ResumoHorarioMeteorologico r where r.tipo = :tipo and r.dataHora between :inicio and :fim order by r.dataHora", ResumoHorarioMeteorologico.class)
                .setParameter("tipo", TipoResumoDiario.HISTORICO).setParameter("inicio", inicio).setParameter("fim", fim).getResultList();
    }

    @Override @Transactional
    public int removerPrevisoesAnterioresA(LocalDateTime dataHora) {
        return sessao().createMutationQuery("delete from ResumoHorarioMeteorologico r where r.tipo = :tipo and r.dataHora < :dataHora")
                .setParameter("tipo", TipoResumoDiario.PREVISAO).setParameter("dataHora", dataHora).executeUpdate();
    }
}
