package br.edu.unip.aps.meteorologia.dto;

import br.edu.unip.aps.meteorologia.model.LeituraMeteorologica;
import br.edu.unip.aps.meteorologia.util.CodigoClimaWMO;

import java.time.LocalDateTime;

/**
 * Formato JSON que o back-end entrega ao front-end (contrato com a página Web).
 * Datas no padrão ISO 8601 (ex.: "2026-10-20T22:20:05"); valores ausentes vêm como {@code null}.
 * Unidades: temperatura °C, umidade %, precipitação e chuva mm, vento km/h.
 */
public record LeituraMeteorologicaDTO(
        Long id,
        LocalDateTime dataHoraColeta,
        LocalDateTime dataHoraFonte,
        Double temperatura,
        Integer umidadeRelativa,
        Double precipitacao,
        Double chuva,
        Double velocidadeVento,
        Integer codigoClima,
        String descricaoClima,
        Boolean presencaPrecipitacao,
        String fonteDados) {

    public static LeituraMeteorologicaDTO de(LeituraMeteorologica leitura) {
        return new LeituraMeteorologicaDTO(
                leitura.getId(),
                leitura.getDataHoraColeta(),
                leitura.getDataHoraFonte(),
                leitura.getTemperatura(),
                leitura.getUmidadeRelativa(),
                leitura.getPrecipitacao(),
                leitura.getChuva(),
                leitura.getVelocidadeVento(),
                leitura.getCodigoClima(),
                CodigoClimaWMO.descrever(leitura.getCodigoClima()),
                leitura.getPresencaPrecipitacao(),
                leitura.getFonteDados());
    }
}
