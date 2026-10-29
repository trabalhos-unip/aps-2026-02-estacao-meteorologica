USE estacao_meteorologica;

-- Migration consolidada para bancos na estrutura anterior à V2.
-- Preserva leituras e resumos já existentes e entrega o modelo final de estação,
-- logs, séries históricas/previsões e probabilidade de chuva.
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
    CONSTRAINT ck_estacao_longitude CHECK (longitude BETWEEN -180 AND 180),
    CONSTRAINT ck_estacao_pais CHECK (pais = 'BR'),
    CONSTRAINT ck_estacao_estado CHECK (estado REGEXP '^[A-Z]{2}$')
) ENGINE=InnoDB;

INSERT INTO estacao (nome, cidade, estado, pais, latitude, longitude)
VALUES ('BR:SP:SOROCABA', 'Sorocaba', 'SP', 'BR', -23.5015, -47.4526)
ON DUPLICATE KEY UPDATE nome = VALUES(nome);

SET @estacao_id = (SELECT id FROM estacao WHERE nome = 'BR:SP:SOROCABA');

ALTER TABLE leitura_meteorologica
    ADD COLUMN estacao_id BIGINT NULL AFTER id,
    ADD COLUMN latitude DOUBLE NULL AFTER codigo_clima,
    ADD COLUMN longitude DOUBLE NULL AFTER latitude,
    ADD COLUMN modo_operacao VARCHAR(10) NOT NULL DEFAULT 'REAL' AFTER longitude,
    ADD COLUMN sensacao_termica DOUBLE NULL AFTER fonte_dados,
    ADD COLUMN pressao_superficie DOUBLE NULL AFTER sensacao_termica,
    ADD COLUMN direcao_vento INT NULL AFTER pressao_superficie,
    ADD COLUMN rajada_vento DOUBLE NULL AFTER direcao_vento,
    ADD COLUMN cobertura_nuvens INT NULL AFTER rajada_vento,
    ADD COLUMN dado_repetido BIT(1) NOT NULL DEFAULT b'0' AFTER cobertura_nuvens;

UPDATE leitura_meteorologica SET estacao_id = @estacao_id WHERE estacao_id IS NULL;

ALTER TABLE leitura_meteorologica
    MODIFY COLUMN estacao_id BIGINT NOT NULL,
    ADD CONSTRAINT fk_leitura_estacao
        FOREIGN KEY (estacao_id) REFERENCES estacao (id)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    ADD CONSTRAINT uq_leitura_estacao_coleta UNIQUE (estacao_id, data_hora_coleta),
    ADD CONSTRAINT ck_leitura_modo CHECK (modo_operacao IN ('REAL', 'SIMULADO')),
    ADD CONSTRAINT ck_leitura_temperatura CHECK (temperatura BETWEEN -90 AND 60),
    ADD CONSTRAINT ck_leitura_umidade CHECK (umidade_relativa BETWEEN 0 AND 100),
    ADD CONSTRAINT ck_leitura_precipitacao CHECK (precipitacao >= 0),
    ADD CONSTRAINT ck_leitura_chuva CHECK (chuva >= 0),
    ADD CONSTRAINT ck_leitura_vento CHECK (velocidade_vento >= 0),
    ADD CONSTRAINT ck_leitura_rajada CHECK (rajada_vento >= 0),
    ADD CONSTRAINT ck_leitura_direcao CHECK (direcao_vento BETWEEN 0 AND 360),
    ADD CONSTRAINT ck_leitura_nuvens CHECK (cobertura_nuvens BETWEEN 0 AND 100),
    ADD CONSTRAINT ck_leitura_pressao CHECK (pressao_superficie BETWEEN 300 AND 1100),
    ADD CONSTRAINT ck_leitura_latitude CHECK (latitude BETWEEN -90 AND 90),
    ADD CONSTRAINT ck_leitura_longitude CHECK (longitude BETWEEN -180 AND 180);

CREATE TABLE log_coleta (
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

ALTER TABLE resumo_diario_meteorologico
    ADD COLUMN estacao_id BIGINT NULL AFTER id,
    ADD COLUMN probabilidade_chuva_maxima INT NULL AFTER precipitacao_total;
UPDATE resumo_diario_meteorologico SET estacao_id = @estacao_id WHERE estacao_id IS NULL;
ALTER TABLE resumo_diario_meteorologico
    MODIFY COLUMN estacao_id BIGINT NOT NULL,
    MODIFY COLUMN tipo VARCHAR(20) NOT NULL,
    DROP INDEX uk_resumo_diario_data_tipo,
    DROP INDEX idx_resumo_diario_data_tipo,
    ADD CONSTRAINT fk_resumo_diario_estacao FOREIGN KEY (estacao_id) REFERENCES estacao (id)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    ADD CONSTRAINT uq_resumo_diario_estacao_data_tipo UNIQUE (estacao_id, data_referencia, tipo),
    ADD CONSTRAINT ck_resumo_diario_tipo CHECK (tipo IN ('HISTORICO', 'PREVISAO')),
    ADD CONSTRAINT ck_resumo_diario_probabilidade_chuva CHECK (probabilidade_chuva_maxima BETWEEN 0 AND 100),
    ADD INDEX idx_resumo_diario_estacao_data_tipo (estacao_id, data_referencia, tipo);

ALTER TABLE resumo_horario_meteorologico
    ADD COLUMN estacao_id BIGINT NULL AFTER id,
    ADD COLUMN probabilidade_chuva INT NULL AFTER chuva;
UPDATE resumo_horario_meteorologico SET estacao_id = @estacao_id WHERE estacao_id IS NULL;
ALTER TABLE resumo_horario_meteorologico
    MODIFY COLUMN estacao_id BIGINT NOT NULL,
    MODIFY COLUMN tipo VARCHAR(20) NOT NULL,
    DROP INDEX uk_resumo_horario_momento_tipo,
    DROP INDEX idx_resumo_horario_momento_tipo,
    ADD CONSTRAINT fk_resumo_horario_estacao FOREIGN KEY (estacao_id) REFERENCES estacao (id)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    ADD CONSTRAINT uq_resumo_horario_estacao_momento_tipo UNIQUE (estacao_id, data_hora_referencia, tipo),
    ADD CONSTRAINT ck_resumo_horario_tipo CHECK (tipo IN ('HISTORICO', 'PREVISAO')),
    ADD CONSTRAINT ck_resumo_horario_probabilidade_chuva CHECK (probabilidade_chuva BETWEEN 0 AND 100),
    ADD INDEX idx_resumo_horario_estacao_momento_tipo (estacao_id, data_hora_referencia, tipo);
