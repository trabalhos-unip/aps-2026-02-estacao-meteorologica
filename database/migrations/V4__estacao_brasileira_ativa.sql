USE estacao_meteorologica;

ALTER TABLE estacao
    ADD COLUMN cidade VARCHAR(100) NULL AFTER nome,
    ADD COLUMN estado VARCHAR(2) NULL AFTER cidade,
    ADD COLUMN pais VARCHAR(2) NOT NULL DEFAULT 'BR' AFTER estado;

UPDATE estacao
SET cidade = COALESCE(cidade, 'Sorocaba'), estado = COALESCE(estado, 'SP'), pais = 'BR'
WHERE cidade IS NULL OR estado IS NULL;

UPDATE estacao SET nome = 'BR:SP:SOROCABA' WHERE id = (SELECT id FROM (SELECT MIN(id) AS id FROM estacao) AS inicial);

ALTER TABLE estacao
    MODIFY COLUMN cidade VARCHAR(100) NOT NULL,
    MODIFY COLUMN estado VARCHAR(2) NOT NULL,
    ADD CONSTRAINT ck_estacao_pais CHECK (pais = 'BR'),
    ADD CONSTRAINT ck_estacao_estado CHECK (estado REGEXP '^[A-Z]{2}$');

-- Uma única estação fica ativa; as demais conservam todo o histórico para consulta futura.
UPDATE estacao SET ativa = (id = (SELECT id FROM (SELECT MIN(id) AS id FROM estacao) AS escolhida));
