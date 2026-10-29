USE estacao_meteorologica;

ALTER TABLE resumo_horario_meteorologico
    ADD COLUMN probabilidade_chuva INT NULL AFTER chuva,
    ADD CONSTRAINT ck_resumo_horario_probabilidade_chuva
        CHECK (probabilidade_chuva BETWEEN 0 AND 100);
