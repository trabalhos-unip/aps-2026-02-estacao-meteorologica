package br.com.aps.meteorologia.app;

import br.com.aps.meteorologia.config.HibernateUtil;
import br.com.aps.meteorologia.controller.LeituraMeteorologicaServlet;
import br.com.aps.meteorologia.dao.ILeituraMeteorologicaDAO;
import br.com.aps.meteorologia.dao.impl.LeituraMeteorologicaDAOHibernate;
import br.com.aps.meteorologia.service.ConsultaMeteorologicaService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PreDestroy;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;

/**
 * Aplicação 2 — Web: Navegador → LeituraMeteorologicaServlet → ConsultaMeteorologicaService
 * → ILeituraMeteorologicaDAO → Hibernate → banco de dados.
 *
 * <p>O Spring Boot sobe um servidor Tomcat embutido (não é preciso instalar Tomcat), registra o
 * {@link LeituraMeteorologicaServlet} em {@code /api/leituras/*} e entrega a página da pasta
 * {@code src/main/webapp} (index.html, css/, js/). Por isso não é necessário {@code WEB-INF/web.xml}.
 * O acesso ao banco NÃO é feito pelo Spring: o DAO usa a {@link HibernateUtil} e o {@code hibernate.cfg.xml}.
 */
@SpringBootApplication(scanBasePackages = "br.com.aps.meteorologia")
public class WebappApplication {

    public static void main(String[] args) {
        SpringApplication.run(WebappApplication.class, args);
    }

    /** Entrega ao Spring a implementação do DAO (Hibernate), para ser usada pelo ConsultaMeteorologicaService. */
    @Bean
    public ILeituraMeteorologicaDAO leituraMeteorologicaDAO() {
        return new LeituraMeteorologicaDAOHibernate();
    }

    /** Registra o Servlet (o mesmo papel do mapeamento no web.xml). */
    @Bean
    public ServletRegistrationBean<LeituraMeteorologicaServlet> leituraMeteorologicaServlet(
            ConsultaMeteorologicaService consultaMeteorologicaService, ObjectMapper conversorJson) {
        return new ServletRegistrationBean<>(
                new LeituraMeteorologicaServlet(consultaMeteorologicaService, conversorJson),
                "/api/leituras/*");
    }

    /** Ao encerrar a aplicação, fecha a SessionFactory do Hibernate. */
    @PreDestroy
    public void encerrar() {
        HibernateUtil.fechar();
    }
}
