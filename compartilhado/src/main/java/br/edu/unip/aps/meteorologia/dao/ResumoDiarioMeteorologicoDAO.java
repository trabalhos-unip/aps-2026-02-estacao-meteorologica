package br.edu.unip.aps.meteorologia.dao;

import br.edu.unip.aps.meteorologia.model.ResumoDiarioMeteorologico;

import java.time.LocalDate;
import java.util.List;

/** Acesso aos resumos de histórico e previsão diários. */
public interface ResumoDiarioMeteorologicoDAO {
    ResumoDiarioMeteorologico salvarOuAtualizar(ResumoDiarioMeteorologico resumo);
    List<ResumoDiarioMeteorologico> buscarHistorico(LocalDate inicio, LocalDate fim);
    List<ResumoDiarioMeteorologico> buscarPrevisoes(LocalDate aPartirDe, int limite);
    int removerPrevisoesAnterioresA(LocalDate data);
}
