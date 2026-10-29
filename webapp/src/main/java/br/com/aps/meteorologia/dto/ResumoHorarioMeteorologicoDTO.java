package br.com.aps.meteorologia.dto;

import br.com.aps.meteorologia.model.ResumoHorarioMeteorologico;
import br.com.aps.meteorologia.model.TipoResumoDiario;

import java.time.LocalDateTime;

/** Resposta pública de um ponto horário histórico ou previsto da Open-Meteo. */
public record ResumoHorarioMeteorologicoDTO(Long id, LocalDateTime dataHora, TipoResumoDiario tipo,
                                             Double temperatura, Integer umidadeRelativa, Double precipitacao,
                                             Double chuva, Integer probabilidadeChuva, Double velocidadeVento,
                                             Integer codigoClima, String fonteDados, LocalDateTime atualizadoEm) {
    public static ResumoHorarioMeteorologicoDTO de(ResumoHorarioMeteorologico resumo) {
        return new ResumoHorarioMeteorologicoDTO(resumo.getId(), resumo.getDataHora(), resumo.getTipo(),
                resumo.getTemperatura(), resumo.getUmidadeRelativa(), resumo.getPrecipitacao(), resumo.getChuva(),
                resumo.getProbabilidadeChuva(), resumo.getVelocidadeVento(), resumo.getCodigoClima(),
                resumo.getFonteDados(), resumo.getAtualizadoEm());
    }
}
