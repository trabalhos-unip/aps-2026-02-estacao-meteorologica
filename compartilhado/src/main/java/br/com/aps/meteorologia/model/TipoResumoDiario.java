package br.com.aps.meteorologia.model;

/** Origem temporal de um resumo diário. Nunca se apresenta uma previsão como observação. */
public enum TipoResumoDiario {
    /** Dia já encerrado, reconstituído pela série passada da Open-Meteo. */
    HISTORICO,
    /** Dia de hoje ou futuro, calculado pelo modelo de previsão da Open-Meteo. */
    PREVISAO
}
