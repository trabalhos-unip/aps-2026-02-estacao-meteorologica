package br.edu.unip.aps.meteorologia.config;

import java.util.logging.Level;
import java.util.logging.Logger;

/** Configura o log padrão do Java (java.util.logging) em uma linha por mensagem. */
public final class Logs {

    /** Referência forte para o nível configurado não se perder. */
    private static final Logger HIBERNATE = Logger.getLogger("org.hibernate");

    private Logs() {
    }

    public static void configurar() {
        System.setProperty("java.util.logging.SimpleFormatter.format", "%1$tF %1$tT %4$-7s %3$s - %5$s%6$s%n");
        HIBERNATE.setLevel(Level.WARNING);
    }
}
