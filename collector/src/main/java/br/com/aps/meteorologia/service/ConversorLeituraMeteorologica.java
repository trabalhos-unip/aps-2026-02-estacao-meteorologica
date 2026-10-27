package br.com.aps.meteorologia.service;

import br.com.aps.meteorologia.client.DadosMeteorologicosAtuais;
import br.com.aps.meteorologia.client.FonteMeteorologicaException;
import br.com.aps.meteorologia.model.LeituraMeteorologica;
import br.com.aps.meteorologia.model.TipoErroColeta;
import br.com.aps.meteorologia.util.DataHoraUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/** Converte os dados normalizados da fonte em uma entidade pronta para persistência. */
@Component
public class ConversorLeituraMeteorologica {

    private static final Logger LOG = LoggerFactory.getLogger(ConversorLeituraMeteorologica.class);

    public LeituraMeteorologica converterParaLeitura(DadosMeteorologicosAtuais dados,
                                                      LocalDateTime dataHoraColeta) {
        validarDadosMeteorologicos(dados);

        LeituraMeteorologica leitura = new LeituraMeteorologica();
        leitura.setDataHoraColeta(dataHoraColeta);
        leitura.setDataHoraFonte(converterDataHoraFonte(dados.dataHoraFonte()));
        leitura.setTemperatura(dados.temperatura());
        leitura.setUmidadeRelativa(dados.umidadeRelativa());
        leitura.setPrecipitacao(dados.precipitacao());
        leitura.setChuva(dados.chuva());
        leitura.setVelocidadeVento(dados.velocidadeVento());
        leitura.setCodigoClima(dados.codigoClima());
        leitura.setLatitude(dados.latitude());
        leitura.setLongitude(dados.longitude());
        leitura.setSensacaoTermica(dados.sensacaoTermica());
        leitura.setPressaoSuperficie(dados.pressaoSuperficie());
        leitura.setDirecaoVento(dados.direcaoVento());
        leitura.setRajadaVento(dados.rajadaVento());
        leitura.setCoberturaNuvens(dados.coberturaNuvens());
        leitura.setPresencaPrecipitacao(
                determinarPresencaPrecipitacao(dados.precipitacao(), dados.chuva()));
        return leitura;
    }

    static Boolean determinarPresencaPrecipitacao(Double precipitacao, Double chuva) {
        if (precipitacao == null && chuva == null) {
            return null;
        }
        return (precipitacao != null && precipitacao > 0) || (chuva != null && chuva > 0);
    }

    private static void validarDadosMeteorologicos(DadosMeteorologicosAtuais dados) {
        if (dados == null) {
            throw new FonteMeteorologicaException(
                    "A fonte não retornou dados meteorológicos", TipoErroColeta.RESPOSTA_INVALIDA);
        }
        if (dados.temperatura() == null && dados.umidadeRelativa() == null
                && dados.precipitacao() == null && dados.velocidadeVento() == null) {
            throw new FonteMeteorologicaException(
                    "Resposta sem nenhuma variável meteorológica", TipoErroColeta.RESPOSTA_INVALIDA);
        }
    }

    private static LocalDateTime converterDataHoraFonte(String dataHoraFonte) {
        try {
            return DataHoraUtil.converterHorarioFonte(dataHoraFonte);
        } catch (IllegalArgumentException erro) {
            LOG.warn("Horário da fonte inválido: {}", dataHoraFonte);
            return null;
        }
    }
}
