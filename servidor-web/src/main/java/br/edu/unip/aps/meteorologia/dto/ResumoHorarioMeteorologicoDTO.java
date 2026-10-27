package br.edu.unip.aps.meteorologia.dto;

import br.edu.unip.aps.meteorologia.model.ResumoHorarioMeteorologico;
import br.edu.unip.aps.meteorologia.model.TipoResumoDiario;
import br.edu.unip.aps.meteorologia.util.CodigoClimaWMO;

import java.time.LocalDateTime;

/** Ponto horário vindo da API para preencher lacunas sem leitura local. */
public record ResumoHorarioMeteorologicoDTO(Long id, LocalDateTime dataHora, TipoResumoDiario tipo,
                                             Double temperatura, Integer umidadeRelativa, Double precipitacao,
                                             Double chuva, Double velocidadeVento, Integer codigoClima,
                                             String descricaoClima, String fonteDados, LocalDateTime atualizadoEm) {
    public static ResumoHorarioMeteorologicoDTO de(ResumoHorarioMeteorologico resumo) {
        return new ResumoHorarioMeteorologicoDTO(resumo.getId(), resumo.getDataHora(), resumo.getTipo(), resumo.getTemperatura(),
                resumo.getUmidadeRelativa(), resumo.getPrecipitacao(), resumo.getChuva(), resumo.getVelocidadeVento(),
                resumo.getCodigoClima(), CodigoClimaWMO.descrever(resumo.getCodigoClima()), resumo.getFonteDados(), resumo.getAtualizadoEm());
    }
}
