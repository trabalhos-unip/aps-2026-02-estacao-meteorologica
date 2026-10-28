package br.com.aps.meteorologia.client;

import br.com.aps.meteorologia.model.ModoOperacao;

/** Abstração para qualquer origem de dados meteorológicos aceita pelo coletor. */
public interface IFonteMeteorologica {

    DadosMeteorologicosAtuais obterDadosAtuais();

    default DadosMeteorologicosAtuais obterDadosAtuais(double latitude, double longitude, String fusoHorario) {
        return obterDadosAtuais();
    }

    ModoOperacao obterModoOperacao();

    String obterNomeFonte();
}
