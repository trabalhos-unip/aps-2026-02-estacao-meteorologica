package br.com.aps.meteorologia.dao;

import br.com.aps.meteorologia.model.ResumoHorarioMeteorologico;

import java.time.LocalDateTime;
import java.util.List;

public interface ResumoHorarioMeteorologicoDAO {
    ResumoHorarioMeteorologico salvarOuAtualizar(ResumoHorarioMeteorologico resumo);
    List<ResumoHorarioMeteorologico> buscarHistorico(Long estacaoId, LocalDateTime inicio, LocalDateTime fim);
    int removerPrevisoesAnterioresA(Long estacaoId, LocalDateTime dataHora);
}
