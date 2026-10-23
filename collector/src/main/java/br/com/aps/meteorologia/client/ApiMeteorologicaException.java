package br.com.aps.meteorologia.client;

/** Falha recuperável ao consultar a API meteorológica (timeout, indisponibilidade, resposta inválida...). */
public class ApiMeteorologicaException extends RuntimeException {

    public ApiMeteorologicaException(String mensagem) {
        super(mensagem);
    }

    public ApiMeteorologicaException(String mensagem, Throwable causa) {
        super(mensagem, causa);
    }
}
