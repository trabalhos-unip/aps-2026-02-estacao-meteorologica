USE estacao_meteorologica;

-- Executar depois de V2 em bancos que já tinham os resumos do painel.
-- As linhas existentes são preservadas e passam a pertencer à estação padrão.
SET @estacao_id = (SELECT id FROM estacao WHERE nome = 'Estacao Sorocaba');

ALTER TABLE resumo_diario_meteorologico
    ADD COLUMN estacao_id BIGINT NULL AFTER id;
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
    ADD INDEX idx_resumo_diario_estacao_data_tipo (estacao_id, data_referencia, tipo);

ALTER TABLE resumo_horario_meteorologico
    ADD COLUMN estacao_id BIGINT NULL AFTER id;
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
    ADD INDEX idx_resumo_horario_estacao_momento_tipo (estacao_id, data_hora_referencia, tipo);
