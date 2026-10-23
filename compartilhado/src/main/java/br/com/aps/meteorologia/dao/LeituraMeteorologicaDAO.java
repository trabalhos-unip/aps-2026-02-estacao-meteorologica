package br.com.aps.meteorologia.dao;

import br.com.aps.meteorologia.model.LeituraMeteorologica;
import br.com.aps.meteorologia.model.ModoOperacao;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Contrato de acesso aos dados das leituras meteorológicas (padrão DAO).
 * Coletor e servidor web dependem desta interface, nunca da implementação.
 *
 * <p>Nos métodos com {@code modo}, o valor {@code null} significa "todos os modos" (REAL e SIMULADO).
 */
public interface LeituraMeteorologicaDAO {

    /** Grava uma nova leitura e devolve a mesma instância já com o id preenchido. */
    LeituraMeteorologica salvar(LeituraMeteorologica leitura);

    /** Leitura mais recente do modo informado (pela data/hora da coleta), ou vazio se não houver. */
    Optional<LeituraMeteorologica> buscarUltima(ModoOperacao modo);

    /**
     * As {@code limite} leituras mais recentes do modo informado, ordenadas da mais antiga para a
     * mais recente (ordem pronta para o gráfico temporal).
     */
    List<LeituraMeteorologica> buscarUltimas(int limite, ModoOperacao modo);

    /**
     * Leituras coletadas entre {@code dataInicio} e {@code dataFim} (inclusive), do modo informado, em ordem
     * cronológica. Se houver mais que {@code limite}, devolve as {@code limite} mais recentes do período.
     */
    List<LeituraMeteorologica> buscarPorPeriodo(LocalDateTime dataInicio, LocalDateTime dataFim,
                                                ModoOperacao modo, int limite);

    /** Quantidade total de leituras gravadas. */
    long contar();

    /** Leitura mais recente, de qualquer modo. */
    default Optional<LeituraMeteorologica> buscarUltima() {
        return buscarUltima(null);
    }

    /** As {@code limite} leituras mais recentes, de qualquer modo, em ordem cronológica. */
    default List<LeituraMeteorologica> buscarUltimas(int limite) {
        return buscarUltimas(limite, null);
    }

    /** Todas as leituras do período, de qualquer modo, em ordem cronológica. */
    default List<LeituraMeteorologica> buscarPorPeriodo(LocalDateTime dataInicio, LocalDateTime dataFim) {
        return buscarPorPeriodo(dataInicio, dataFim, null, Integer.MAX_VALUE);
    }
}
