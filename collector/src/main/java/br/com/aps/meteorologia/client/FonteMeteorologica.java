package br.com.aps.meteorologia.client;

import br.com.aps.meteorologia.model.ModoOperacao;

/**
 * De onde vêm as condições meteorológicas do coletor.
 *
 * <p>Duas implementações (polimorfismo):
 * <ul>
 *   <li>{@link ApiMeteorologicaClient} — modo REAL, consulta a Open-Meteo;</li>
 *   <li>{@link SimuladorMeteorologico} — modo SIMULADO, gera cenários de teste.</li>
 * </ul>
 * O Spring escolhe qual usar pela propriedade {@code coleta.modo}. As duas devolvem o mesmo formato
 * ({@link RespostaOpenMeteo}), então passam pela mesma validação, conversão e gravação.
 */
public interface FonteMeteorologica {

    /**
     * Obtém as condições atuais.
     *
     * @throws ApiMeteorologicaException em falha recuperável (rede, HTTP, resposta inválida)
     */
    RespostaOpenMeteo buscarCondicoesAtuais();

    /** Modo gravado em cada leitura obtida desta fonte. */
    ModoOperacao modo();

    /** Nome gravado no campo fonteDados (ex.: "Open-Meteo"). */
    String nomeFonte();
}
