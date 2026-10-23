package br.com.aps.meteorologia.model;

import br.com.aps.meteorologia.util.CodigoClimaWMO;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

import java.time.LocalDateTime;

/**
 * Uma leitura meteorológica obtida da API Open-Meteo e mapeada para a tabela
 * {@code leitura_meteorologica} via Hibernate.
 *
 * <p>Os valores numéricos usam tipos wrapper ({@link Double}, {@link Integer}) para que
 * um valor ausente na resposta da API seja gravado como {@code NULL}, e não como zero.
 *
 * <p>Importante: a Open-Meteo entrega dados de modelos meteorológicos com resolução de
 * 15 minutos. Por isso guardamos dois horários: {@link #dataHoraColeta} (quando o sistema
 * consultou a API) e {@link #dataHoraFonte} (o {@code current.time} retornado pela API).
 */
@Entity
@Table(name = "leitura_meteorologica",
        indexes = @Index(name = "idx_leitura_data_hora_coleta", columnList = "data_hora_coleta"))
public class LeituraMeteorologica {

    public static final String FONTE_OPEN_METEO = "Open-Meteo";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Instante em que o coletor consultou a API e salvou a leitura. */
    @Column(name = "data_hora_coleta", nullable = false)
    private LocalDateTime dataHoraColeta;

    /** Horário ({@code current.time}) associado aos dados retornados pela API. */
    @Column(name = "data_hora_fonte")
    private LocalDateTime dataHoraFonte;

    /** {@code temperature_2m}, em °C. */
    @Column(name = "temperatura")
    private Double temperatura;

    /** {@code relative_humidity_2m}, em %. */
    @Column(name = "umidade_relativa")
    private Integer umidadeRelativa;

    /** {@code precipitation}, em mm. */
    @Column(name = "precipitacao")
    private Double precipitacao;

    /** {@code rain}, em mm. */
    @Column(name = "chuva")
    private Double chuva;

    /** {@code wind_speed_10m}, em km/h. */
    @Column(name = "velocidade_vento")
    private Double velocidadeVento;

    /** {@code weather_code}, código WMO da condição do tempo. */
    @Column(name = "codigo_clima")
    private Integer codigoClima;

    /** Indicador derivado: {@code true} quando precipitação ou chuva é maior que zero. */
    @Column(name = "presenca_precipitacao")
    private Boolean presencaPrecipitacao;

    @Column(name = "fonte_dados", nullable = false, length = 50)
    private String fonteDados = FONTE_OPEN_METEO;

    /** Construtor exigido pelo Hibernate. */
    public LeituraMeteorologica() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LocalDateTime getDataHoraColeta() {
        return dataHoraColeta;
    }

    public void setDataHoraColeta(LocalDateTime dataHoraColeta) {
        this.dataHoraColeta = dataHoraColeta;
    }

    public LocalDateTime getDataHoraFonte() {
        return dataHoraFonte;
    }

    public void setDataHoraFonte(LocalDateTime dataHoraFonte) {
        this.dataHoraFonte = dataHoraFonte;
    }

    public Double getTemperatura() {
        return temperatura;
    }

    public void setTemperatura(Double temperatura) {
        this.temperatura = temperatura;
    }

    public Integer getUmidadeRelativa() {
        return umidadeRelativa;
    }

    public void setUmidadeRelativa(Integer umidadeRelativa) {
        this.umidadeRelativa = umidadeRelativa;
    }

    public Double getPrecipitacao() {
        return precipitacao;
    }

    public void setPrecipitacao(Double precipitacao) {
        this.precipitacao = precipitacao;
    }

    public Double getChuva() {
        return chuva;
    }

    public void setChuva(Double chuva) {
        this.chuva = chuva;
    }

    public Double getVelocidadeVento() {
        return velocidadeVento;
    }

    public void setVelocidadeVento(Double velocidadeVento) {
        this.velocidadeVento = velocidadeVento;
    }

    public Integer getCodigoClima() {
        return codigoClima;
    }

    public void setCodigoClima(Integer codigoClima) {
        this.codigoClima = codigoClima;
    }

    /** Descrição em português do código WMO (ex.: 3 → "Nublado"). Calculada, não é gravada no banco. */
    @Transient
    public String getDescricaoClima() {
        return CodigoClimaWMO.descrever(codigoClima);
    }

    public Boolean getPresencaPrecipitacao() {
        return presencaPrecipitacao;
    }

    public void setPresencaPrecipitacao(Boolean presencaPrecipitacao) {
        this.presencaPrecipitacao = presencaPrecipitacao;
    }

    public String getFonteDados() {
        return fonteDados;
    }

    public void setFonteDados(String fonteDados) {
        this.fonteDados = fonteDados;
    }

    @Override
    public String toString() {
        return "LeituraMeteorologica{id=" + id
                + ", dataHoraColeta=" + dataHoraColeta
                + ", dataHoraFonte=" + dataHoraFonte
                + ", temperatura=" + temperatura
                + ", umidadeRelativa=" + umidadeRelativa
                + ", precipitacao=" + precipitacao
                + ", chuva=" + chuva
                + ", velocidadeVento=" + velocidadeVento
                + ", codigoClima=" + codigoClima
                + ", presencaPrecipitacao=" + presencaPrecipitacao + '}';
    }
}
