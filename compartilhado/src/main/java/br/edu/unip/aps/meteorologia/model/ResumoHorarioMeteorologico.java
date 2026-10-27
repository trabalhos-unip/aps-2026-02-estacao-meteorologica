package br.edu.unip.aps.meteorologia.model;

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

import java.time.LocalDateTime;

/** Série horária da Open-Meteo para cobrir intervalos em que o coletor local ficou desligado. */
@Entity
@Table(name = "resumo_horario_meteorologico",
        uniqueConstraints = @UniqueConstraint(name = "uk_resumo_horario_momento_tipo", columnNames = {"data_hora_referencia", "tipo"}),
        indexes = @Index(name = "idx_resumo_horario_momento_tipo", columnList = "data_hora_referencia,tipo"))
public class ResumoHorarioMeteorologico {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "data_hora_referencia", nullable = false)
    private LocalDateTime dataHora;

    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20)
    private TipoResumoDiario tipo;

    private Double temperatura;
    @Column(name = "umidade_relativa") private Integer umidadeRelativa;
    private Double precipitacao;
    private Double chuva;
    @Column(name = "velocidade_vento") private Double velocidadeVento;
    @Column(name = "codigo_clima") private Integer codigoClima;
    @Column(name = "fonte_dados", nullable = false, length = 100) private String fonteDados;
    @Column(name = "atualizado_em", nullable = false) private LocalDateTime atualizadoEm;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public LocalDateTime getDataHora() { return dataHora; }
    public void setDataHora(LocalDateTime dataHora) { this.dataHora = dataHora; }
    public TipoResumoDiario getTipo() { return tipo; }
    public void setTipo(TipoResumoDiario tipo) { this.tipo = tipo; }
    public Double getTemperatura() { return temperatura; }
    public void setTemperatura(Double temperatura) { this.temperatura = temperatura; }
    public Integer getUmidadeRelativa() { return umidadeRelativa; }
    public void setUmidadeRelativa(Integer umidadeRelativa) { this.umidadeRelativa = umidadeRelativa; }
    public Double getPrecipitacao() { return precipitacao; }
    public void setPrecipitacao(Double precipitacao) { this.precipitacao = precipitacao; }
    public Double getChuva() { return chuva; }
    public void setChuva(Double chuva) { this.chuva = chuva; }
    public Double getVelocidadeVento() { return velocidadeVento; }
    public void setVelocidadeVento(Double velocidadeVento) { this.velocidadeVento = velocidadeVento; }
    public Integer getCodigoClima() { return codigoClima; }
    public void setCodigoClima(Integer codigoClima) { this.codigoClima = codigoClima; }
    public String getFonteDados() { return fonteDados; }
    public void setFonteDados(String fonteDados) { this.fonteDados = fonteDados; }
    public LocalDateTime getAtualizadoEm() { return atualizadoEm; }
    public void setAtualizadoEm(LocalDateTime atualizadoEm) { this.atualizadoEm = atualizadoEm; }
}
