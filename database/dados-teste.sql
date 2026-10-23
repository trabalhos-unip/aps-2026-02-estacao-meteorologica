-- Dados de teste para abrir a página/gráfico sem precisar rodar o coletor.
-- Leituras REAIS de exemplo (incluindo chuva e um registro com valores ausentes)
-- e leituras SIMULADAS com os cenários do simulador (tempo seco, chuva forte, vento forte, dados incompletos).
USE estacao_meteorologica;

INSERT INTO leitura_meteorologica
    (data_hora_coleta, data_hora_fonte, temperatura, umidade_relativa, precipitacao, chuva,
     velocidade_vento, codigo_clima, latitude, longitude, modo_operacao, presenca_precipitacao, fonte_dados)
VALUES
    -- REAL
    ('2026-10-20 14:00:00', '2026-10-20 14:00:00', 24.1, 62,   0.0,  0.0,  11.2, 2,    -23.514938, -46.610504, 'REAL',     0,    'Open-Meteo'),
    ('2026-10-20 14:00:10', '2026-10-20 14:00:00', 24.1, 62,   0.0,  0.0,  11.2, 2,    -23.514938, -46.610504, 'REAL',     0,    'Open-Meteo'),
    ('2026-10-20 14:15:00', '2026-10-20 14:15:00', 23.6, 66,   0.0,  0.0,  12.0, 3,    -23.514938, -46.610504, 'REAL',     0,    'Open-Meteo'),
    ('2026-10-20 14:30:00', '2026-10-20 14:30:00', 22.8, 74,   0.4,  0.4,  14.5, 61,   -23.514938, -46.610504, 'REAL',     1,    'Open-Meteo'),
    ('2026-10-20 14:45:00', '2026-10-20 14:45:00', 21.9, 81,   1.2,  1.2,  16.1, 63,   -23.514938, -46.610504, 'REAL',     1,    'Open-Meteo'),
    ('2026-10-20 15:00:00', NULL,                  NULL, NULL, NULL, NULL, NULL, NULL, -23.514938, -46.610504, 'REAL',     NULL, 'Open-Meteo'),
    -- SIMULADO (mesmos valores dos cenários do SimuladorMeteorologico)
    ('2026-10-20 16:00:00', '2026-10-20 16:00:00', 31.0, 28,   0.0,  0.0,  8.0,  0,    -23.5505,   -46.6333,   'SIMULADO', 0,    'Simulador'),
    ('2026-10-20 16:00:10', '2026-10-20 16:00:00', 19.5, 96,   8.2,  8.2,  22.0, 65,   -23.5505,   -46.6333,   'SIMULADO', 1,    'Simulador'),
    ('2026-10-20 16:00:20', '2026-10-20 16:00:00', 22.0, 70,   0.0,  0.0,  68.0, 3,    -23.5505,   -46.6333,   'SIMULADO', 0,    'Simulador'),
    ('2026-10-20 16:00:30', '2026-10-20 16:00:00', 24.0, NULL, NULL, NULL, NULL, NULL, -23.5505,   -46.6333,   'SIMULADO', NULL, 'Simulador');
