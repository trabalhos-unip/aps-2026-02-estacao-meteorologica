package br.com.aps.meteorologia.client;

/**
 * Condições meteorológicas usadas pelo {@link SimuladorMeteorologico} nos testes e na demonstração
 * (item "e" do enunciado: simular condições meteorológicas para verificar a coleta e a publicação dos dados).
 *
 * <p>Valores nas mesmas unidades da Open-Meteo: °C, %, mm, km/h e código WMO. {@code null} = valor ausente.
 */
public enum CenarioSimulado {

    /** Calor e ar seco, sem chuva. */
    TEMPO_SECO(31.0, 28, 0.0, 0.0, 8.0, 0),

    /** Chuva forte com umidade alta. */
    CHUVA_FORTE(19.5, 96, 8.2, 8.2, 22.0, 65),

    /** Vento forte sem chuva. */
    VENTO_FORTE(22.0, 70, 0.0, 0.0, 68.0, 3),

    /** Resposta incompleta: só a temperatura chega; o restante fica ausente (desconhecido). */
    DADOS_INCOMPLETOS(24.0, null, null, null, null, null);

    private final Double temperatura;
    private final Integer umidadeRelativa;
    private final Double precipitacao;
    private final Double chuva;
    private final Double velocidadeVento;
    private final Integer codigoClima;

    CenarioSimulado(Double temperatura, Integer umidadeRelativa, Double precipitacao, Double chuva,
                    Double velocidadeVento, Integer codigoClima) {
        this.temperatura = temperatura;
        this.umidadeRelativa = umidadeRelativa;
        this.precipitacao = precipitacao;
        this.chuva = chuva;
        this.velocidadeVento = velocidadeVento;
        this.codigoClima = codigoClima;
    }

    public Double temperatura() {
        return temperatura;
    }

    public Integer umidadeRelativa() {
        return umidadeRelativa;
    }

    public Double precipitacao() {
        return precipitacao;
    }

    public Double chuva() {
        return chuva;
    }

    public Double velocidadeVento() {
        return velocidadeVento;
    }

    public Integer codigoClima() {
        return codigoClima;
    }
}
