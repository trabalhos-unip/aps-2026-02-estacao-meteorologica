package br.com.aps.meteorologia.app;

import br.com.aps.meteorologia.config.HibernateUtil;
import br.com.aps.meteorologia.dao.LeituraMeteorologicaDAO;
import br.com.aps.meteorologia.dao.impl.LeituraMeteorologicaDAOImpl;
import jakarta.annotation.PreDestroy;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Aplicação 1 — Coletor: API meteorológica → ApiMeteorologicaClient → ColetaMeteorologicaService
 * → LeituraMeteorologica → LeituraMeteorologicaDAO → Hibernate → banco de dados.
 *
 * <p>O Spring Boot inicia a aplicação, lê o {@code application.properties}, cria os objetos
 * (client, service, DAO) e executa a coleta a cada 10 segundos ({@code @EnableScheduling} + {@code @Scheduled}).
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
    public LeituraMeteorologicaDAO leituraMeteorologicaDAO() {
        return new LeituraMeteorologicaDAOImpl();
    }

    /** Ao encerrar a aplicação, fecha a SessionFactory do Hibernate. */
    @PreDestroy
    public void encerrar() {
        HibernateUtil.fechar();
    }
}
