package br.com.aps.meteorologia.dao;

import br.com.aps.meteorologia.model.ResumoDiarioMeteorologico;

import java.time.LocalDate;
import java.util.List;

/** Acesso aos resumos de histórico e previsão diários. */
public interface ResumoDiarioMeteorologicoDAO {
    ResumoDiarioMeteorologico salvarOuAtualizar(ResumoDiarioMeteorologico resumo);
    List<ResumoDiarioMeteorologico> buscarHistorico(Long estacaoId, LocalDate inicio, LocalDate fim);
    List<ResumoDiarioMeteorologico> buscarPrevisoes(Long estacaoId, LocalDate aPartirDe, int limite);
    int removerPrevisoesAnterioresA(Long estacaoId, LocalDate data);
}
