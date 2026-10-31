package br.com.aps.meteorologia.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Resumo de um dia retornado pela Open-Meteo. Esta entidade é propositalmente
 * separada das leituras instantâneas: dias passados e dias previstos têm
 * significados diferentes e não devem ser confundidos no histórico da estação.
 */
@Entity
@Table(name = "resumo_diario_meteorologico",
        uniqueConstraints = @UniqueConstraint(name = "uk_resumo_diario_estacao_data_tipo", columnNames = {"estacao_id", "data_referencia", "tipo"}),
        indexes = @Index(name = "idx_resumo_diario_estacao_data_tipo", columnList = "estacao_id,data_referencia,tipo"))
public class ResumoDiarioMeteorologico {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "estacao_id", nullable = false)
    private Long estacaoId;

    @Column(name = "data_referencia", nullable = false)
    private LocalDate data;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 20)
    private TipoResumoDiario tipo;

    @Column(name = "temperatura_minima")
    private Double temperaturaMinima;

    @Column(name = "temperatura_maxima")
    private Double temperaturaMaxima;

    @Column(name = "temperatura_media")
    private Double temperaturaMedia;

    @Column(name = "umidade_media")
    private Double umidadeMedia;

    @Column(name = "precipitacao_total")
    private Double precipitacaoTotal;

    @Column(name = "probabilidade_chuva_maxima")
    private Integer probabilidadeChuvaMaxima;

    @Column(name = "vento_maximo")
    private Double ventoMaximo;

    @Column(name = "codigo_clima")
    private Integer codigoClima;

    @Column(name = "fonte_dados", nullable = false, length = 100)
    private String fonteDados;

    @Column(name = "atualizado_em", nullable = false)
    private LocalDateTime atualizadoEm;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getEstacaoId() { return estacaoId; }
    public void setEstacaoId(Long estacaoId) { this.estacaoId = estacaoId; }
    public LocalDate getData() { return data; }
    public void setData(LocalDate data) { this.data = data; }
    public TipoResumoDiario getTipo() { return tipo; }
    public void setTipo(TipoResumoDiario tipo) { this.tipo = tipo; }
    public Double getTemperaturaMinima() { return temperaturaMinima; }
    public void setTemperaturaMinima(Double temperaturaMinima) { this.temperaturaMinima = temperaturaMinima; }
    public Double getTemperaturaMaxima() { return temperaturaMaxima; }
    public void setTemperaturaMaxima(Double temperaturaMaxima) { this.temperaturaMaxima = temperaturaMaxima; }
    public Double getTemperaturaMedia() { return temperaturaMedia; }
    public void setTemperaturaMedia(Double temperaturaMedia) { this.temperaturaMedia = temperaturaMedia; }
    public Double getUmidadeMedia() { return umidadeMedia; }
    public void setUmidadeMedia(Double umidadeMedia) { this.umidadeMedia = umidadeMedia; }
    public Double getPrecipitacaoTotal() { return precipitacaoTotal; }
    public void setPrecipitacaoTotal(Double precipitacaoTotal) { this.precipitacaoTotal = precipitacaoTotal; }
    public Integer getProbabilidadeChuvaMaxima() { return probabilidadeChuvaMaxima; }
    public void setProbabilidadeChuvaMaxima(Integer probabilidadeChuvaMaxima) { this.probabilidadeChuvaMaxima = probabilidadeChuvaMaxima; }
    public Double getVentoMaximo() { return ventoMaximo; }
    public void setVentoMaximo(Double ventoMaximo) { this.ventoMaximo = ventoMaximo; }
    public Integer getCodigoClima() { return codigoClima; }
    public void setCodigoClima(Integer codigoClima) { this.codigoClima = codigoClima; }
    public String getFonteDados() { return fonteDados; }
    public void setFonteDados(String fonteDados) { this.fonteDados = fonteDados; }
    public LocalDateTime getAtualizadoEm() { return atualizadoEm; }
    public void setAtualizadoEm(LocalDateTime atualizadoEm) { this.atualizadoEm = atualizadoEm; }
}
