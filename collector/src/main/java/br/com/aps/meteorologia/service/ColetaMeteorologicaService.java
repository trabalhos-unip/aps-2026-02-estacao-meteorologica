package br.com.aps.meteorologia.service;

import br.com.aps.meteorologia.client.DadosMeteorologicosAtuais;
import br.com.aps.meteorologia.client.FonteMeteorologicaException;
import br.com.aps.meteorologia.client.IFonteMeteorologica;
import br.com.aps.meteorologia.dao.IEstacaoDAO;
import br.com.aps.meteorologia.dao.ILeituraMeteorologicaDAO;
import br.com.aps.meteorologia.dao.ILogColetaDAO;
import br.com.aps.meteorologia.dao.PersistenciaException;
import br.com.aps.meteorologia.model.LeituraMeteorologica;
import br.com.aps.meteorologia.model.LogColeta;
import br.com.aps.meteorologia.model.StatusColeta;
import br.com.aps.meteorologia.model.TipoErroColeta;
import br.com.aps.meteorologia.util.DataHoraUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;

/** Orquestra a obtenção, conversão e persistência de cada leitura meteorológica. */
@Service
public class ColetaMeteorologicaService {

    private static final Logger LOG = LoggerFactory.getLogger(ColetaMeteorologicaService.class);
    private static final int TAMANHO_MAXIMO_MENSAGEM_LOG = 500;

    private final IFonteMeteorologica fonteMeteorologica;
    private final ILeituraMeteorologicaDAO leituraMeteorologicaDAO;
    private final IEstacaoDAO estacaoDAO;
    private final ILogColetaDAO logColetaDAO;
    private final ConversorLeituraMeteorologica conversorLeitura;
    private final ZoneId fusoHorario;
    private final String nomeEstacao;
    private final double latitudeEstacao;
    private final double longitudeEstacao;

    private Long estacaoId;
    private LocalDateTime ultimaDataHoraFonte;

    public ColetaMeteorologicaService(IFonteMeteorologica fonteMeteorologica,
                                      ILeituraMeteorologicaDAO leituraMeteorologicaDAO,
                                      IEstacaoDAO estacaoDAO,
                                      ILogColetaDAO logColetaDAO,
                                      ConversorLeituraMeteorologica conversorLeitura,
                                      @Value("${estacao.nome}") String nomeEstacao,
                                      @Value("${openmeteo.latitude}") double latitudeEstacao,
                                      @Value("${openmeteo.longitude}") double longitudeEstacao,
                                      @Value("${openmeteo.timezone}") String fusoHorario) {
        this.fonteMeteorologica = fonteMeteorologica;
        this.leituraMeteorologicaDAO = leituraMeteorologicaDAO;
        this.estacaoDAO = estacaoDAO;
        this.logColetaDAO = logColetaDAO;
        this.conversorLeitura = conversorLeitura;
        this.nomeEstacao = nomeEstacao;
        this.latitudeEstacao = latitudeEstacao;
        this.longitudeEstacao = longitudeEstacao;
        this.fusoHorario = ZoneId.of(fusoHorario);
    }

    @Scheduled(fixedDelayString = "${coleta.intervalo-ms:10000}")
    public void executarColetaAgendada() {
        long inicioColeta = System.nanoTime();
        try {
            LeituraMeteorologica leitura = coletarESalvarLeitura();
            registrarColetaComSucesso(leitura, calcularDuracaoEmMilissegundos(inicioColeta));
            LOG.info("Leitura #{} salva [{}]", leitura.getId(), leitura.getModoOperacao());
        } catch (FonteMeteorologicaException erro) {
            registrarFalhaNaColeta(erro, erro.getTipoErro(), erro.getCodigoHttp(),
                    calcularDuracaoEmMilissegundos(inicioColeta));
            LOG.warn("Coleta ignorada: {}", erro.getMessage());
        } catch (PersistenciaException erro) {
            registrarFalhaNaColeta(erro, TipoErroColeta.BANCO, null,
                    calcularDuracaoEmMilissegundos(inicioColeta));
            LOG.error("Coleta ignorada: {}", erro.getMessage());
        } catch (RuntimeException erro) {
            registrarFalhaNaColeta(erro, TipoErroColeta.OUTRO, null,
                    calcularDuracaoEmMilissegundos(inicioColeta));
            LOG.error("Coleta ignorada", erro);
        }
    }

    public LeituraMeteorologica coletarESalvarLeitura() {
        DadosMeteorologicosAtuais dadosAtuais = fonteMeteorologica.obterDadosAtuais();
        LeituraMeteorologica leitura = conversorLeitura.converterParaLeitura(
                dadosAtuais, DataHoraUtil.agora(fusoHorario));
        leitura.setEstacaoId(obterEstacaoId());
        leitura.setModoOperacao(fonteMeteorologica.obterModoOperacao());
        leitura.setFonteDados(fonteMeteorologica.obterNomeFonte());
        leitura.setDadoRepetido(leitura.getDataHoraFonte() != null
                && leitura.getDataHoraFonte().equals(ultimaDataHoraFonte));

        LeituraMeteorologica leituraSalva = leituraMeteorologicaDAO.salvar(leitura);
        ultimaDataHoraFonte = leituraSalva.getDataHoraFonte();
        return leituraSalva;
    }

    private synchronized Long obterEstacaoId() {
        if (estacaoId == null) {
            estacaoId = estacaoDAO.buscarOuCriar(nomeEstacao, latitudeEstacao, longitudeEstacao).getId();
        }
        return estacaoId;
    }

    private void registrarColetaComSucesso(LeituraMeteorologica leitura, int duracaoMilissegundos) {
        try {
            LogColeta logColeta = criarLogColeta(StatusColeta.SUCESSO, duracaoMilissegundos);
            logColeta.setLeituraId(leitura.getId());
            logColetaDAO.salvar(logColeta);
        } catch (RuntimeException erro) {
            LOG.warn("Não foi possível registrar o log da coleta: {}", erro.getMessage());
        }
    }

    private void registrarFalhaNaColeta(RuntimeException erro, TipoErroColeta tipoErro,
                                        Integer codigoHttp, int duracaoMilissegundos) {
        try {
            LogColeta logColeta = criarLogColeta(StatusColeta.ERRO, duracaoMilissegundos);
            logColeta.setTipoErro(tipoErro);
            logColeta.setHttpStatus(codigoHttp);
            logColeta.setMensagem(resumirMensagem(erro.getMessage()));
            logColetaDAO.salvar(logColeta);
        } catch (RuntimeException falhaAoRegistrarLog) {
            LOG.warn("Não foi possível registrar o log da coleta: {}", falhaAoRegistrarLog.getMessage());
        }
    }

    private LogColeta criarLogColeta(StatusColeta status, int duracaoMilissegundos) {
        LogColeta logColeta = new LogColeta();
        logColeta.setEstacaoId(obterEstacaoId());
        logColeta.setDataHora(DataHoraUtil.agora(fusoHorario));
        logColeta.setModoOperacao(fonteMeteorologica.obterModoOperacao());
        logColeta.setStatus(status);
        logColeta.setDuracaoMs(duracaoMilissegundos);
        return logColeta;
    }

    private static int calcularDuracaoEmMilissegundos(long inicioEmNanossegundos) {
        return (int) ((System.nanoTime() - inicioEmNanossegundos) / 1_000_000);
    }

    private static String resumirMensagem(String mensagem) {
        if (mensagem == null) {
            return null;
        }
        return mensagem.length() <= TAMANHO_MAXIMO_MENSAGEM_LOG
                ? mensagem
                : mensagem.substring(0, TAMANHO_MAXIMO_MENSAGEM_LOG);
    }
}
