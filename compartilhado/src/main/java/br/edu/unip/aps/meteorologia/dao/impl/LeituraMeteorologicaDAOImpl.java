package br.edu.unip.aps.meteorologia.dao.impl;

import br.edu.unip.aps.meteorologia.dao.LeituraMeteorologicaDAO;
import br.edu.unip.aps.meteorologia.model.LeituraMeteorologica;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.hibernate.Session;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Implementação do DAO com Hibernate. Toda operação de banco do sistema fica concentrada aqui.
 *
 * <p>O Spring Boot cria a {@code SessionFactory} do Hibernate a partir do
 * {@code application.properties} e abre/fecha a sessão e a transação de cada método
 * marcado com {@link Transactional} (commit em caso de sucesso, rollback em caso de exceção).
 * Por ser um {@link Repository}, as exceções do Hibernate são convertidas em
 * {@code DataAccessException} do Spring.
 */
@Repository
@Transactional(readOnly = true)
public class LeituraMeteorologicaDAOImpl implements LeituraMeteorologicaDAO {

    @PersistenceContext
    private EntityManager entityManager;

    /** Sessão do Hibernate associada à transação atual. */
    private Session sessao() {
        return entityManager.unwrap(Session.class);
    }

    @Override
    @Transactional
    public LeituraMeteorologica salvar(LeituraMeteorologica leitura) {
        if (leitura == null) {
            throw new IllegalArgumentException("A leitura não pode ser nula");
        }
        sessao().persist(leitura);
        return leitura;
    }

    @Override
    public Optional<LeituraMeteorologica> buscarUltima() {
        return sessao()
                .createQuery("from LeituraMeteorologica l order by l.dataHoraColeta desc, l.id desc",
                        LeituraMeteorologica.class)
                .setMaxResults(1)
                .uniqueResultOptional();
    }

    @Override
    public List<LeituraMeteorologica> buscarUltimas(int limite) {
        if (limite <= 0) {
            throw new IllegalArgumentException("O limite deve ser maior que zero");
        }
        List<LeituraMeteorologica> maisRecentesPrimeiro = sessao()
                .createQuery("from LeituraMeteorologica l order by l.dataHoraColeta desc, l.id desc",
                        LeituraMeteorologica.class)
                .setMaxResults(limite)
                .getResultList();
        List<LeituraMeteorologica> cronologica = new ArrayList<>(maisRecentesPrimeiro);
        Collections.reverse(cronologica);
        return cronologica;
    }

    @Override
    public List<LeituraMeteorologica> buscarPorPeriodo(LocalDateTime dataInicio, LocalDateTime dataFim) {
        if (dataInicio == null || dataFim == null) {
            throw new IllegalArgumentException("Data de início e data de fim são obrigatórias");
        }
        if (dataInicio.isAfter(dataFim)) {
            throw new IllegalArgumentException("A data de início deve ser anterior à data de fim");
        }
        return sessao()
                .createQuery("from LeituraMeteorologica l"
                                + " where l.dataHoraColeta between :inicio and :fim"
                                + " order by l.dataHoraColeta asc, l.id asc",
                        LeituraMeteorologica.class)
                .setParameter("inicio", dataInicio)
                .setParameter("fim", dataFim)
                .getResultList();
    }

    @Override
    public long contar() {
        return sessao()
                .createQuery("select count(l) from LeituraMeteorologica l", Long.class)
                .getSingleResult();
    }
}
