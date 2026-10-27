package br.com.aps.meteorologia.client;

/**
 * Dados normalizados usados pelo caso de uso de coleta.
 * A aplicação trabalha com este formato sem depender do JSON específico da Open-Meteo.
 */
public record DadosMeteorologicosAtuais(
        Double latitude,
        Double longitude,
        String dataHoraFonte,
        Double temperatura,
        Integer umidadeRelativa,
        Double precipitacao,
        Double chuva,
        Double velocidadeVento,
        Integer codigoClima,
        Double sensacaoTermica,
        Double pressaoSuperficie,
        Integer direcaoVento,
        Double rajadaVento,
        Integer coberturaNuvens) {
}
