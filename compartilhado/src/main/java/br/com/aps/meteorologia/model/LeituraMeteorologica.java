package br.com.aps.meteorologia.model;

import br.com.aps.meteorologia.util.CodigoClimaWMO;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

/**
 * Uma leitura meteorológica (real, da API Open-Meteo, ou simulada) mapeada para a tabela
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
        indexes = {
                @Index(name = "idx_leitura_data_hora_coleta", columnList = "data_hora_coleta"),
                @Index(name = "idx_leitura_modo_data_hora", columnList = "modo_operacao, data_hora_coleta")
        })
public class LeituraMeteorologica {

    public static final String FONTE_OPEN_METEO = "Open-Meteo";
    public static final String FONTE_SIMULADOR = "Simulador";

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

    /** Latitude da localização dos dados, em graus (a Open-Meteo devolve a do ponto de grade do modelo). */
    @Column(name = "latitude")
    private Double latitude;

    /** Longitude da localização dos dados, em graus. */
    @Column(name = "longitude")
    private Double longitude;

    /** REAL (Open-Meteo) ou SIMULADO (cenário de teste). */
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR) // VARCHAR(10), igual ao schema.sql (sem isso o MySQL usaria o tipo ENUM)
    @ColumnDefault("'REAL'")
    @Column(name = "modo_operacao", nullable = false, length = 10)
    private ModoOperacao modoOperacao = ModoOperacao.REAL;

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

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public ModoOperacao getModoOperacao() {
        return modoOperacao;
    }

    public void setModoOperacao(ModoOperacao modoOperacao) {
        this.modoOperacao = modoOperacao;
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
                + ", latitude=" + latitude
                + ", longitude=" + longitude
                + ", modoOperacao=" + modoOperacao
                + ", presencaPrecipitacao=" + presencaPrecipitacao + '}';
    }
}
