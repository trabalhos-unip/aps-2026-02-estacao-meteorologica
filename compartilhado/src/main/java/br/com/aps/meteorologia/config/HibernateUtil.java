package br.com.aps.meteorologia.config;

import br.com.aps.meteorologia.dao.PersistenciaException;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;

/**
 * Cria e guarda a única {@link SessionFactory} do Hibernate da aplicação.
 *
 * <p>Lê o {@code hibernate.cfg.xml} e completa a URL, o usuário e a senha do banco a partir do
 * {@code .env} (classe {@link Configuracao}), para que a senha nunca fique no repositório.
 *
 * <p>A fábrica é criada na primeira vez que for pedida. Se o banco estiver fora do ar, é lançada uma
 * {@link PersistenciaException} e a próxima chamada tenta conectar de novo (o coletor não para por isso).
 */
public final class HibernateUtil {

    private static SessionFactory sessionFactory;

    private HibernateUtil() {
    }

    public static synchronized SessionFactory getSessionFactory() {
        if (sessionFactory == null || sessionFactory.isClosed()) {
            try {
                Configuration configuration = new Configuration();
                configuration.configure(); // lê src/main/resources/hibernate.cfg.xml
                configuration.setProperty("hibernate.connection.url", Configuracao.urlBanco());
                configuration.setProperty("hibernate.connection.username", Configuracao.usuarioBanco());
                configuration.setProperty("hibernate.connection.password", Configuracao.senhaBanco());
                aplicarSobrescritas(configuration);
                sessionFactory = configuration.buildSessionFactory();
            } catch (RuntimeException e) {
                throw new PersistenciaException("Não foi possível conectar ao banco de dados: " + e.getMessage(), e);
            }
        }
        return sessionFactory;
    }

    /** Fecha a SessionFactory (chamado ao encerrar a aplicação). */
    public static synchronized void fechar() {
        if (sessionFactory != null && !sessionFactory.isClosed()) {
            sessionFactory.close();
        }
        sessionFactory = null;
    }

    /**
     * Propriedades de sistema que começam com "hibernate." substituem as do XML.
     * Usado pelos testes para trocar o MySQL por um banco H2 em memória.
     */
    private static void aplicarSobrescritas(Configuration configuration) {
        for (String chave : System.getProperties().stringPropertyNames()) {
            if (chave.startsWith("hibernate.")) {
                configuration.setProperty(chave, System.getProperty(chave));
            }
        }
    }
}
