USE estacao_meteorologica;

ALTER TABLE resumo_diario_meteorologico
    ADD COLUMN probabilidade_chuva_maxima INT NULL AFTER precipitacao_total,
    ADD CONSTRAINT ck_resumo_diario_probabilidade_chuva
        CHECK (probabilidade_chuva_maxima BETWEEN 0 AND 100);
