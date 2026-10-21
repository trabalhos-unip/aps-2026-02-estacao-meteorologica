package br.edu.unip.aps.meteorologia;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Aplicação 1 — Coletor: API Open-Meteo → ApiMeteorologicaClient → ColetaMeteorologicaService
 * → LeituraMeteorologicaDAO → Hibernate → MySQL.
 *
 * <p>Fica no pacote raiz para que o Spring encontre também a entidade e o DAO do módulo compartilhado.
 */
@SpringBootApplication
@EnableScheduling
public class ColetorApplication {

    public static void main(String[] args) {
        SpringApplication.run(ColetorApplication.class, args);
    }
}
