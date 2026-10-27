-- Estação Meteorológica — APS 2026/2
-- DDL MySQL da tabela de leituras. Corresponde à entidade
-- br.edu.unip.aps.meteorologia.model.LeituraMeteorologica (módulo compartilhado).
--
-- Proposta inicial do back-end para revisão do responsável pelo banco.
-- Enquanto spring.jpa.hibernate.ddl-auto=update, o Hibernate cria esta tabela sozinho;
-- depois de aprovado este script, mude para "validate" em compartilhado/src/main/resources/banco.properties.

CREATE DATABASE IF NOT EXISTS estacao_meteorologica
    CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE estacao_meteorologica;

CREATE TABLE IF NOT EXISTS leitura_meteorologica (
    id                    BIGINT       NOT NULL AUTO_INCREMENT,
    data_hora_coleta      DATETIME(6)  NOT NULL COMMENT 'Quando o coletor consultou a API',
    data_hora_fonte       DATETIME(6)  NULL     COMMENT 'current.time da Open-Meteo (resolução de 15 min)',
    temperatura           DOUBLE       NULL     COMMENT 'temperature_2m (°C)',
    umidade_relativa      INT          NULL     COMMENT 'relative_humidity_2m (%)',
    precipitacao          DOUBLE       NULL     COMMENT 'precipitation (mm)',
    chuva                 DOUBLE       NULL     COMMENT 'rain (mm)',
    velocidade_vento      DOUBLE       NULL     COMMENT 'wind_speed_10m (km/h)',
    codigo_clima          INT          NULL     COMMENT 'weather_code (código WMO)',
    presenca_precipitacao BIT(1)       NULL     COMMENT '1 quando precipitation > 0 ou rain > 0',
    fonte_dados           VARCHAR(50)  NOT NULL DEFAULT 'Open-Meteo',
    PRIMARY KEY (id),
    INDEX idx_leitura_data_hora_coleta (data_hora_coleta)
) ENGINE = InnoDB;

-- Resumos diários baixados pelo coletor na inicialização. Esta tabela NÃO substitui
-- leitura_meteorologica: HISTORICO representa dias já passados e PREVISAO representa
-- hoje e os próximos dias, para a interface não confundir previsão com medição local.
CREATE TABLE IF NOT EXISTS resumo_diario_meteorologico (
    id                    BIGINT       NOT NULL AUTO_INCREMENT,
    data_referencia       DATE         NOT NULL,
    tipo                  VARCHAR(20)  NOT NULL COMMENT 'HISTORICO ou PREVISAO',
    temperatura_minima    DOUBLE       NULL COMMENT 'menor temperature_2m do dia (°C)',
    temperatura_maxima    DOUBLE       NULL COMMENT 'maior temperature_2m do dia (°C)',
    temperatura_media     DOUBLE       NULL COMMENT 'média de temperature_2m do dia (°C)',
    umidade_media         DOUBLE       NULL COMMENT 'média de relative_humidity_2m do dia (%)',
    precipitacao_total    DOUBLE       NULL COMMENT 'soma de precipitation do dia (mm)',
    vento_maximo          DOUBLE       NULL COMMENT 'maior wind_speed_10m do dia (km/h)',
    codigo_clima          INT          NULL COMMENT 'weather_code próximo ao meio-dia',
    fonte_dados           VARCHAR(100) NOT NULL,
    atualizado_em         DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_resumo_diario_data_tipo (data_referencia, tipo),
    INDEX idx_resumo_diario_data_tipo (data_referencia, tipo)
) ENGINE = InnoDB;
