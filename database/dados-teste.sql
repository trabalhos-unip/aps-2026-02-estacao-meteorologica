USE estacao_meteorologica;

SET @estacao_id = (SELECT id FROM estacao WHERE nome = 'Estacao Sorocaba');

INSERT INTO leitura_meteorologica
    (estacao_id, data_hora_coleta, data_hora_fonte, temperatura, umidade_relativa,
     precipitacao, chuva, velocidade_vento, codigo_clima, latitude, longitude,
     modo_operacao, presenca_precipitacao, fonte_dados, sensacao_termica,
     pressao_superficie, direcao_vento, rajada_vento, cobertura_nuvens, dado_repetido)
VALUES
    (@estacao_id, '2026-10-20 14:00:00', '2026-10-20 14:00:00', 24.1, 62, 0.0, 0.0, 11.2, 2,
     -23.5015, -47.4526, 'REAL', 0, 'Open-Meteo', 23.5, 1014.3, 135, 18.0, 35, 0),
    (@estacao_id, '2026-10-20 14:00:10', '2026-10-20 14:00:00', 24.1, 62, 0.0, 0.0, 11.2, 2,
     -23.5015, -47.4526, 'REAL', 0, 'Open-Meteo', 23.5, 1014.3, 135, 18.0, 35, 1),
    (@estacao_id, '2026-10-20 14:15:00', '2026-10-20 14:15:00', 23.6, 66, 0.0, 0.0, 12.0, 3,
     -23.5015, -47.4526, 'REAL', 0, 'Open-Meteo', 23.1, 1014.8, 150, 19.5, 48, 0),
    (@estacao_id, '2026-10-20 14:30:00', '2026-10-20 14:30:00', 22.8, 74, 0.4, 0.4, 14.5, 61,
     -23.5015, -47.4526, 'REAL', 1, 'Open-Meteo', 22.4, 1015.1, 170, 24.0, 78, 0),
    (@estacao_id, '2026-10-20 14:45:00', '2026-10-20 14:45:00', 21.9, 81, 1.2, 1.2, 16.1, 63,
     -23.5015, -47.4526, 'REAL', 1, 'Open-Meteo', 21.5, 1015.6, 185, 26.0, 92, 0),
    (@estacao_id, '2026-10-20 16:00:00', '2026-10-20 16:00:00', 31.0, 28, 0.0, 0.0, 8.0, 0,
     -23.5015, -47.4526, 'SIMULADO', 0, 'Simulador', NULL, NULL, NULL, NULL, NULL, 0),
    (@estacao_id, '2026-10-20 16:00:10', '2026-10-20 16:00:00', 19.5, 96, 8.2, 8.2, 22.0, 65,
     -23.5015, -47.4526, 'SIMULADO', 1, 'Simulador', NULL, NULL, NULL, NULL, NULL, 1),
    (@estacao_id, '2026-10-20 16:00:20', '2026-10-20 16:00:00', 22.0, 70, 0.0, 0.0, 68.0, 3,
     -23.5015, -47.4526, 'SIMULADO', 0, 'Simulador', NULL, NULL, NULL, NULL, NULL, 1),
    (@estacao_id, '2026-10-20 16:00:30', NULL, 24.0, NULL, NULL, NULL, NULL, NULL,
     -23.5015, -47.4526, 'SIMULADO', NULL, 'Simulador', NULL, NULL, NULL, NULL, NULL, 0);
