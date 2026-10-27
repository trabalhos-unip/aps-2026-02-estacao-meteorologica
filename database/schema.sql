CREATE DATABASE IF NOT EXISTS estacao_meteorologica
    CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE estacao_meteorologica;

CREATE TABLE IF NOT EXISTS estacao (
    id           BIGINT       NOT NULL AUTO_INCREMENT,
    nome         VARCHAR(100) NOT NULL,
    cidade       VARCHAR(100) NOT NULL,
    estado       VARCHAR(2)   NOT NULL,
    pais         VARCHAR(2)   NOT NULL DEFAULT 'BR',
    latitude     DOUBLE       NOT NULL,
    longitude    DOUBLE       NOT NULL,
    fuso_horario VARCHAR(50)  NOT NULL DEFAULT 'America/Sao_Paulo',
    ativa        BIT(1)       NOT NULL DEFAULT b'1',
    criada_em    DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT uq_estacao_nome UNIQUE (nome),
    CONSTRAINT ck_estacao_latitude CHECK (latitude BETWEEN -90 AND 90),
    CONSTRAINT ck_estacao_longitude CHECK (longitude BETWEEN -180 AND 180)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS leitura_meteorologica (
    id                    BIGINT       NOT NULL AUTO_INCREMENT,
    estacao_id            BIGINT       NOT NULL,
    data_hora_coleta      DATETIME(6)  NOT NULL,
    data_hora_fonte       DATETIME(6)  NULL,
    temperatura           DOUBLE       NULL,
    umidade_relativa      INT          NULL,
    precipitacao          DOUBLE       NULL,
    chuva                 DOUBLE       NULL,
    velocidade_vento      DOUBLE       NULL,
    codigo_clima          INT          NULL,
    latitude              DOUBLE       NULL,
    longitude             DOUBLE       NULL,
    modo_operacao         VARCHAR(10)  NOT NULL DEFAULT 'REAL',
    presenca_precipitacao BIT(1)       NULL,
    fonte_dados           VARCHAR(50)  NOT NULL,
    sensacao_termica      DOUBLE       NULL,
    pressao_superficie    DOUBLE       NULL,
    direcao_vento         INT          NULL,
    rajada_vento          DOUBLE       NULL,
    cobertura_nuvens      INT          NULL,
    dado_repetido         BIT(1)       NOT NULL DEFAULT b'0',
    PRIMARY KEY (id),
    CONSTRAINT fk_leitura_estacao
        FOREIGN KEY (estacao_id) REFERENCES estacao (id)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT uq_leitura_estacao_coleta UNIQUE (estacao_id, data_hora_coleta),
    CONSTRAINT ck_leitura_modo CHECK (modo_operacao IN ('REAL', 'SIMULADO')),
    CONSTRAINT ck_leitura_temperatura CHECK (temperatura BETWEEN -90 AND 60),
    CONSTRAINT ck_leitura_umidade CHECK (umidade_relativa BETWEEN 0 AND 100),
    CONSTRAINT ck_leitura_precipitacao CHECK (precipitacao >= 0),
    CONSTRAINT ck_leitura_chuva CHECK (chuva >= 0),
    CONSTRAINT ck_leitura_vento CHECK (velocidade_vento >= 0),
    CONSTRAINT ck_leitura_rajada CHECK (rajada_vento >= 0),
    CONSTRAINT ck_leitura_direcao CHECK (direcao_vento BETWEEN 0 AND 360),
    CONSTRAINT ck_leitura_nuvens CHECK (cobertura_nuvens BETWEEN 0 AND 100),
    CONSTRAINT ck_leitura_pressao CHECK (pressao_superficie BETWEEN 300 AND 1100),
    CONSTRAINT ck_leitura_latitude CHECK (latitude BETWEEN -90 AND 90),
    CONSTRAINT ck_leitura_longitude CHECK (longitude BETWEEN -180 AND 180),
    INDEX idx_leitura_data_hora_coleta (data_hora_coleta),
    INDEX idx_leitura_modo_data_hora (modo_operacao, data_hora_coleta)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS log_coleta (
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    estacao_id    BIGINT       NOT NULL,
    data_hora     DATETIME(6)  NOT NULL,
    modo_operacao VARCHAR(10)  NOT NULL DEFAULT 'REAL',
    status        VARCHAR(10)  NOT NULL,
    tipo_erro     VARCHAR(20)  NULL,
    http_status   INT          NULL,
    mensagem      VARCHAR(500) NULL,
    duracao_ms    INT          NULL,
    leitura_id    BIGINT       NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_log_estacao
        FOREIGN KEY (estacao_id) REFERENCES estacao (id)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT fk_log_leitura
        FOREIGN KEY (leitura_id) REFERENCES leitura_meteorologica (id)
        ON UPDATE CASCADE ON DELETE SET NULL,
    CONSTRAINT ck_log_modo CHECK (modo_operacao IN ('REAL', 'SIMULADO')),
    CONSTRAINT ck_log_status CHECK (status IN ('SUCESSO', 'ERRO')),
    CONSTRAINT ck_log_tipo CHECK (tipo_erro IS NULL OR tipo_erro IN
        ('TIMEOUT', 'REDE', 'LIMITE_API', 'HTTP', 'RESPOSTA_INVALIDA', 'BANCO', 'OUTRO')),
    CONSTRAINT ck_log_status_erro CHECK (
        (status = 'SUCESSO' AND tipo_erro IS NULL) OR
        (status = 'ERRO' AND tipo_erro IS NOT NULL)
    ),
    INDEX idx_log_data (data_hora),
    INDEX idx_log_status_data (status, data_hora)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS resumo_diario_meteorologico (
    id                  BIGINT       NOT NULL AUTO_INCREMENT,
    estacao_id          BIGINT       NOT NULL,
    data_referencia     DATE         NOT NULL,
    tipo                VARCHAR(20)  NOT NULL,
    temperatura_minima  DOUBLE       NULL,
    temperatura_maxima  DOUBLE       NULL,
    temperatura_media   DOUBLE       NULL,
    umidade_media       DOUBLE       NULL,
    precipitacao_total  DOUBLE       NULL,
    vento_maximo        DOUBLE       NULL,
    codigo_clima        INT          NULL,
    fonte_dados         VARCHAR(100) NOT NULL,
    atualizado_em       DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_resumo_diario_estacao FOREIGN KEY (estacao_id) REFERENCES estacao (id)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT uq_resumo_diario_estacao_data_tipo UNIQUE (estacao_id, data_referencia, tipo),
    CONSTRAINT ck_resumo_diario_tipo CHECK (tipo IN ('HISTORICO', 'PREVISAO')),
    INDEX idx_resumo_diario_estacao_data_tipo (estacao_id, data_referencia, tipo)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS resumo_horario_meteorologico (
    id                    BIGINT       NOT NULL AUTO_INCREMENT,
    estacao_id            BIGINT       NOT NULL,
    data_hora_referencia  DATETIME(6)  NOT NULL,
    tipo                  VARCHAR(20)  NOT NULL,
    temperatura           DOUBLE       NULL,
    umidade_relativa      INT          NULL,
    precipitacao          DOUBLE       NULL,
    chuva                 DOUBLE       NULL,
    velocidade_vento      DOUBLE       NULL,
    codigo_clima          INT          NULL,
    fonte_dados           VARCHAR(100) NOT NULL,
    atualizado_em         DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_resumo_horario_estacao FOREIGN KEY (estacao_id) REFERENCES estacao (id)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT uq_resumo_horario_estacao_momento_tipo UNIQUE (estacao_id, data_hora_referencia, tipo),
    CONSTRAINT ck_resumo_horario_tipo CHECK (tipo IN ('HISTORICO', 'PREVISAO')),
    INDEX idx_resumo_horario_estacao_momento_tipo (estacao_id, data_hora_referencia, tipo)
) ENGINE=InnoDB;

INSERT INTO estacao (nome, cidade, estado, pais, latitude, longitude)
VALUES ('BR:SP:SOROCABA', 'Sorocaba', 'SP', 'BR', -23.5015, -47.4526)
ON DUPLICATE KEY UPDATE nome = VALUES(nome);
