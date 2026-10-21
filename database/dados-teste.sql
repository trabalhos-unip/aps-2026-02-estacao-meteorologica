-- Dados de teste para abrir a página/gráfico sem precisar rodar o coletor.
-- Inclui um registro com valores ausentes (NULL) e um com chuva.
USE estacao_meteorologica;

INSERT INTO leitura_meteorologica
    (data_hora_coleta, data_hora_fonte, temperatura, umidade_relativa, precipitacao, chuva,
     velocidade_vento, codigo_clima, presenca_precipitacao, fonte_dados)
VALUES
    ('2026-10-20 14:00:00', '2026-10-20 14:00:00', 24.1, 62, 0.0, 0.0, 11.2, 2,  0, 'Open-Meteo'),
    ('2026-10-20 14:00:10', '2026-10-20 14:00:00', 24.1, 62, 0.0, 0.0, 11.2, 2,  0, 'Open-Meteo'),
    ('2026-10-20 14:15:00', '2026-10-20 14:15:00', 23.6, 66, 0.0, 0.0, 12.0, 3,  0, 'Open-Meteo'),
    ('2026-10-20 14:30:00', '2026-10-20 14:30:00', 22.8, 74, 0.4, 0.4, 14.5, 61, 1, 'Open-Meteo'),
    ('2026-10-20 14:45:00', '2026-10-20 14:45:00', 21.9, 81, 1.2, 1.2, 16.1, 63, 1, 'Open-Meteo'),
    ('2026-10-20 15:00:00', NULL,                  NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'Open-Meteo');
