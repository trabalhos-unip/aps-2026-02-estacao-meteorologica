package br.com.aps.meteorologia.app;

import br.com.aps.meteorologia.config.HibernateUtil;
import br.com.aps.meteorologia.dao.IEstacaoDAO;
import br.com.aps.meteorologia.dao.ILeituraMeteorologicaDAO;
import br.com.aps.meteorologia.dao.ILogColetaDAO;
import br.com.aps.meteorologia.dao.impl.EstacaoDAOHibernate;
import br.com.aps.meteorologia.dao.impl.LeituraMeteorologicaDAOHibernate;
import br.com.aps.meteorologia.dao.impl.LogColetaDAOHibernate;
import jakarta.annotation.PreDestroy;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Aplicação 1 — Coletor: fonte meteorológica → ColetaMeteorologicaService
 * → LeituraMeteorologica → ILeituraMeteorologicaDAO → Hibernate → banco de dados.
 *
 * <p>O Spring Boot inicia a aplicação, lê o {@code application.properties}, cria a fonte, o serviço e os DAOs
 * e executa a coleta a cada 10 segundos ({@code @EnableScheduling} + {@code @Scheduled}).
 * O acesso ao banco NÃO é feito pelo Spring: o DAO usa a {@link HibernateUtil} e o {@code hibernate.cfg.xml}.
 */
@SpringBootApplication(scanBasePackages = "br.com.aps.meteorologia")
@EnableScheduling
public class CollectorApplication {

    public static void main(String[] args) {
        SpringApplication.run(CollectorApplication.class, args);
    }

    /** Entrega ao Spring a implementação do DAO (Hibernate), para ser usada pelo ColetaMeteorologicaService. */
    @Bean
    public ILeituraMeteorologicaDAO leituraMeteorologicaDAO() {
        return new LeituraMeteorologicaDAOHibernate();
    }

    @Bean
    public IEstacaoDAO estacaoDAO() {
        return new EstacaoDAOHibernate();
    }

    @Bean
    public ILogColetaDAO logColetaDAO() {
        return new LogColetaDAOHibernate();
    }

    /** Ao encerrar a aplicação, fecha a SessionFactory do Hibernate. */
    @PreDestroy
    public void encerrar() {
        HibernateUtil.fechar();
    }
}
