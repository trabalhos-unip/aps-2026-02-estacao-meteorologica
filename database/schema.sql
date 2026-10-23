-- Estação Meteorológica — APS 2026/2
-- DDL MySQL da tabela de leituras. Corresponde à entidade
-- br.com.aps.meteorologia.model.LeituraMeteorologica (módulo compartilhado).
--
-- Proposta inicial do back-end para revisão do responsável pelo banco.
-- Enquanto hibernate.hbm2ddl.auto=update (hibernate.cfg.xml), o Hibernate cria esta tabela sozinho;
-- depois de aprovado este script, mude para "validate" em compartilhado/src/main/resources/hibernate.cfg.xml.

CREATE DATABASE IF NOT EXISTS estacao_meteorologica
    CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE estacao_meteorologica;

CREATE TABLE IF NOT EXISTS leitura_meteorologica (
    id                    BIGINT       NOT NULL AUTO_INCREMENT,
    data_hora_coleta      DATETIME(6)  NOT NULL COMMENT 'Quando o coletor consultou a fonte',
    data_hora_fonte       DATETIME(6)  NULL     COMMENT 'current.time da Open-Meteo (resolução de 15 min)',
    temperatura           DOUBLE       NULL     COMMENT 'temperature_2m (°C)',
    umidade_relativa      INT          NULL     COMMENT 'relative_humidity_2m (%)',
    precipitacao          DOUBLE       NULL     COMMENT 'precipitation (mm)',
    chuva                 DOUBLE       NULL     COMMENT 'rain (mm)',
    velocidade_vento      DOUBLE       NULL     COMMENT 'wind_speed_10m (km/h)',
    codigo_clima          INT          NULL     COMMENT 'weather_code (código WMO)',
    latitude              DOUBLE       NULL     COMMENT 'latitude dos dados (graus)',
    longitude             DOUBLE       NULL     COMMENT 'longitude dos dados (graus)',
    modo_operacao         VARCHAR(10)  NOT NULL DEFAULT 'REAL' COMMENT 'REAL (Open-Meteo) ou SIMULADO (cenário de teste)',
    presenca_precipitacao BIT(1)       NULL     COMMENT '1 quando precipitation > 0 ou rain > 0',
    fonte_dados           VARCHAR(50)  NOT NULL DEFAULT 'Open-Meteo' COMMENT 'Open-Meteo ou Simulador',
    PRIMARY KEY (id),
    INDEX idx_leitura_data_hora_coleta (data_hora_coleta),
    INDEX idx_leitura_modo_data_hora (modo_operacao, data_hora_coleta)
) ENGINE = InnoDB;

-- Para quem já criou a tabela com a versão anterior deste script (sem latitude, longitude e modo):
-- ALTER TABLE leitura_meteorologica
--     ADD COLUMN latitude      DOUBLE      NULL AFTER codigo_clima,
--     ADD COLUMN longitude     DOUBLE      NULL AFTER latitude,
--     ADD COLUMN modo_operacao VARCHAR(10) NOT NULL DEFAULT 'REAL' AFTER longitude,
--     ADD INDEX idx_leitura_modo_data_hora (modo_operacao, data_hora_coleta);
