package br.com.aps.meteorologia.model;

public enum TipoErroColeta {
    TIMEOUT,
    REDE,
    LIMITE_API,
    HTTP,
    RESPOSTA_INVALIDA,
    BANCO,
    OUTRO
}
