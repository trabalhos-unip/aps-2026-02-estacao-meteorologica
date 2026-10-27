package br.edu.unip.aps.meteorologia.dao;

import br.edu.unip.aps.meteorologia.model.ResumoHorarioMeteorologico;

import java.time.LocalDateTime;
import java.util.List;

public interface ResumoHorarioMeteorologicoDAO {
    ResumoHorarioMeteorologico salvarOuAtualizar(ResumoHorarioMeteorologico resumo);
    List<ResumoHorarioMeteorologico> buscarHistorico(LocalDateTime inicio, LocalDateTime fim);
    int removerPrevisoesAnterioresA(LocalDateTime dataHora);
}
