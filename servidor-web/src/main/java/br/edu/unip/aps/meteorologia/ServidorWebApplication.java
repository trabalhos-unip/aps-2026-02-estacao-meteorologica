package br.edu.unip.aps.meteorologia;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Aplicação 2 — Web: Navegador → LeituraMeteorologicaController → ConsultaMeteorologicaService
 * → LeituraMeteorologicaDAO → Hibernate → MySQL.
 *
 * <p>Fica no pacote raiz para que o Spring encontre também a entidade e o DAO do módulo compartilhado.
 */
@SpringBootApplication
public class ServidorWebApplication {

    public static void main(String[] args) {
        SpringApplication.run(ServidorWebApplication.class, args);
    }
}
