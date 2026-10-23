package br.com.aps.meteorologia.dao;

import br.com.aps.meteorologia.model.LeituraMeteorologica;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Contrato de acesso aos dados das leituras meteorológicas (padrão DAO).
 * Coletor e servidor web dependem desta interface, nunca da implementação.
 */
public interface LeituraMeteorologicaDAO {

    /** Grava uma nova leitura e devolve a mesma instância já com o id preenchido. */
    LeituraMeteorologica salvar(LeituraMeteorologica leitura);

    /** Leitura mais recente (pela data/hora da coleta), ou vazio se o banco não tem leituras. */
    Optional<LeituraMeteorologica> buscarUltima();

    /**
     * As {@code limite} leituras mais recentes, ordenadas da mais antiga para a mais recente
     * (ordem pronta para o gráfico temporal).
     */
    List<LeituraMeteorologica> buscarUltimas(int limite);

    /** Leituras coletadas entre {@code dataInicio} e {@code dataFim} (inclusive), em ordem cronológica. */
    List<LeituraMeteorologica> buscarPorPeriodo(LocalDateTime dataInicio, LocalDateTime dataFim);

    /** Quantidade total de leituras gravadas. */
    long contar();
}
