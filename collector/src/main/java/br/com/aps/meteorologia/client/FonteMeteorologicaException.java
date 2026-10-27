package br.com.aps.meteorologia.client;

import br.com.aps.meteorologia.model.TipoErroColeta;

/** Falha recuperável ao obter dados de uma fonte meteorológica. */
public class FonteMeteorologicaException extends RuntimeException {

    private final TipoErroColeta tipoErro;
    private final Integer codigoHttp;

    public FonteMeteorologicaException(String mensagem, TipoErroColeta tipoErro) {
        this(mensagem, tipoErro, null, null);
    }

    public FonteMeteorologicaException(String mensagem, TipoErroColeta tipoErro, Throwable causa) {
        this(mensagem, tipoErro, null, causa);
    }

    public FonteMeteorologicaException(String mensagem, TipoErroColeta tipoErro, Integer codigoHttp) {
        this(mensagem, tipoErro, codigoHttp, null);
    }

    private FonteMeteorologicaException(String mensagem, TipoErroColeta tipoErro,
                                        Integer codigoHttp, Throwable causa) {
        super(mensagem, causa);
        this.tipoErro = tipoErro;
        this.codigoHttp = codigoHttp;
    }

    public TipoErroColeta getTipoErro() {
        return tipoErro;
    }

    public Integer getCodigoHttp() {
        return codigoHttp;
    }
}
