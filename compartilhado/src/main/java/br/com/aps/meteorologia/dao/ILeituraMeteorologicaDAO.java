package br.com.aps.meteorologia.dao;

import br.com.aps.meteorologia.model.LeituraMeteorologica;
import br.com.aps.meteorologia.model.ModoOperacao;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Contrato de persistência das leituras meteorológicas.
 * Nos métodos com {@code modoOperacao}, o valor {@code null} inclui os modos REAL e SIMULADO.
 */
public interface ILeituraMeteorologicaDAO {

    LeituraMeteorologica salvar(LeituraMeteorologica leitura);

    Optional<LeituraMeteorologica> buscarUltima(ModoOperacao modoOperacao);

    List<LeituraMeteorologica> buscarUltimas(int limite, ModoOperacao modoOperacao);

    List<LeituraMeteorologica> buscarPorPeriodo(LocalDateTime dataInicio, LocalDateTime dataFim,
                                                ModoOperacao modoOperacao, int limite);

    long contarTodas();

    default Optional<LeituraMeteorologica> buscarUltima() {
        return buscarUltima(null);
    }

    default List<LeituraMeteorologica> buscarUltimas(int limite) {
        return buscarUltimas(limite, null);
    }

    default List<LeituraMeteorologica> buscarPorPeriodo(LocalDateTime dataInicio, LocalDateTime dataFim) {
        return buscarPorPeriodo(dataInicio, dataFim, null, Integer.MAX_VALUE);
    }
}
