package br.edu.unip.aps.meteorologia.dto;

import br.edu.unip.aps.meteorologia.model.ResumoDiarioMeteorologico;
import br.edu.unip.aps.meteorologia.model.TipoResumoDiario;
import br.edu.unip.aps.meteorologia.util.CodigoClimaWMO;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** Contrato JSON para os dias históricos e para os dias previstos. */
public record ResumoDiarioMeteorologicoDTO(
        Long id,
        LocalDate data,
        TipoResumoDiario tipo,
        Double temperaturaMinima,
        Double temperaturaMaxima,
        Double temperaturaMedia,
        Double umidadeMedia,
        Double precipitacaoTotal,
        Double ventoMaximo,
        Integer codigoClima,
        String descricaoClima,
        String fonteDados,
        LocalDateTime atualizadoEm) {

    public static ResumoDiarioMeteorologicoDTO de(ResumoDiarioMeteorologico resumo) {
        return new ResumoDiarioMeteorologicoDTO(resumo.getId(), resumo.getData(), resumo.getTipo(),
                resumo.getTemperaturaMinima(), resumo.getTemperaturaMaxima(), resumo.getTemperaturaMedia(),
                resumo.getUmidadeMedia(), resumo.getPrecipitacaoTotal(), resumo.getVentoMaximo(),
                resumo.getCodigoClima(), CodigoClimaWMO.descrever(resumo.getCodigoClima()),
                resumo.getFonteDados(), resumo.getAtualizadoEm());
    }
}
