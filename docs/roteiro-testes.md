# Roteiro de testes

Para cada teste: entrada/procedimento, resultado esperado, resultado obtido, data e evidência.
Evidências (capturas de tela, trechos de log, consultas ao banco) ficam em [`docs/evidencias/`](evidencias/).
Regra do planejamento: **não marcar um teste como executado antes de realizá-lo**.

## 1. Testes automatizados (`mvn test`)

Rodam sem MySQL e sem internet. Resultado de 22/10/2026: **51 testes, 0 falhas**.

| Módulo | Classe de teste | O que verifica |
|---|---|---|
| compartilhado | `LeituraMeteorologicaDAOHibernateTest` (10) | DAO real com `HibernateUtil` + H2 (modo MySQL): salvar e recuperar, banco vazio, valores ausentes (`NULL`), últimas leituras em ordem cronológica, consulta por período, coordenadas e modo gravados, modo padrão REAL, **filtro por modo**, **limite na consulta por período**, validação de parâmetros |
| collector | `ClienteOpenMeteoTest` (7) | cliente HTTP contra servidor local que imita a Open-Meteo: resposta real convertida, URL com todas as variáveis, HTTP 429 (limite), HTTP 503 (indisponível), JSON inválido, resposta sem `current`, sem conexão |
| collector | `SimuladorMeteorologicoTest` (7) | alternância dos 4 cenários, cenário fixo, variação de valores, mesmo formato da API, dados incompletos como ausentes, modo/fonte, cenário inválido |
| collector | `ColetaMeteorologicaServiceTest` (9) | conversão API → entidade, coordenadas/modo/fonte gravados, **coleta simulada pelo mesmo caminho da real**, valores ausentes, resposta sem variáveis rejeitada, regra de presença de precipitação, falha da fonte e falha do banco **não derrubam** o coletor |
| collector | `DataHoraUtilTest` (4) | conversão do `current.time`, horário vazio, formato inválido |
| collector | `CollectorApplicationTest` (2) | Spring Boot do coletor sobe; usa a API no modo REAL (padrão) e o simulador no modo SIMULADO |
| webapp | `LeituraMeteorologicaServletTest` (12) | Spring Boot + Tomcat embutido reais: última leitura (com latitude, longitude e modo), banco vazio (404), últimas N, limite inválido (400), período, **filtro por modo**, modo inválido (400), período > 31 dias (400), data inválida/ausente (400), banco indisponível (503), total, entrega de `index.html`, `css` e `js` |

## 2. Testes manuais — estratégia mínima do planejamento

| # | Teste | Procedimento | Resultado esperado | Resultado obtido | Data | Evidência |
|---|---|---|---|---|---|---|
| 1 | Open-Meteo disponível | Chamar a Forecast API com as coordenadas de São Paulo e as 6 variáveis | JSON válido; campos e unidades identificados | ✅ JSON com `temperature_2m` (°C), `relative_humidity_2m` (%), `precipitation`/`rain` (mm), `wind_speed_10m` (km/h), `weather_code` e `interval: 900` | 20/10/2026 | — |
| 2 | Persistência | Coletar uma leitura e consultar o banco | Registro salvo e recuperável | ✅ com **H2 no lugar do MySQL**: leituras gravadas pelo collector e lidas pelo webapp. ⏳ Repetir com MySQL | 22/10/2026 | — |
| 3 | Periodicidade | Executar o coletor por alguns minutos | Coletas a cada ~10 s | ✅ (curto, com H2): coletas REAL às 23:23:40, 23:23:53, 23:24:03. ⏳ Repetir por 2+ min com MySQL | 22/10/2026 | — |
| 4 | Consulta histórica | Buscar várias leituras por data/hora | Ordem temporal definida | ✅ teste automatizado do DAO e `GET /api/leituras/periodo?...&limite=2` → ids 11, 12 (as mais recentes, em ordem cronológica) | 22/10/2026 | — |
| 5 | Gráfico | Abrir a página com registros existentes | Gráfico com eixos/unidades legíveis | ⏳ Pendente: `grafico.js` ainda não implementado (front-end). A página já recebe os dados | — | — |
| 6 | Banco vazio | Abrir a página antes de haver leituras | Mensagem informativa; página não quebra | ✅ no back-end: `/api/leituras/ultima` → 404 "Nenhuma leitura registrada ainda" (teste automatizado). ⏳ Conferir a interface final | 22/10/2026 | — |
| 7 | Falha da API | Simular desconexão ou resposta inválida | Erro tratado e registrado; sistema não encerra | ✅ testes automatizados (429, 503, JSON inválido, sem conexão) | 22/10/2026 | — |
| 7b | Falha do banco | Iniciar o coletor com o MySQL desligado | Erro registrado; coletor continua e reconecta | ✅ na versão sem Spring (mesmo `HibernateUtil`/DAO): erro registrado a cada ciclo, sem encerrar. ⏳ Repetir nesta versão | 21/10/2026 | — |
| 8 | Integração completa | Iniciar banco, coletor e web e abrir a página | Dados aparecem na interface/gráfico | ✅ com H2: API → collector → banco → Servlet → página. ⏳ Repetir com MySQL e gráfico | 22/10/2026 | — |
| 9 | Execução limpa | Seguir só o README num ambiente do zero | Outro integrante inicia sem ajuda | ⏳ Pendente | — | — |

✅ executado · ⏳ pendente

## 3. Simulação de condições meteorológicas (item "e" do enunciado)

O coletor tem um **modo SIMULADO** que gera condições de teste sem acessar a internet. As leituras passam pela mesma
validação, conversão e gravação das reais e ficam marcadas com `modoOperacao = SIMULADO` e `fonteDados = "Simulador"`.

Como executar: no `.env`, `COLETA_MODO=SIMULADO` (e, se quiser um cenário só, `COLETA_CENARIO=CHUVA_FORTE`).
Detalhes em [`scripts/iniciar-coletor.md`](../scripts/iniciar-coletor.md).

| Cenário | Valores base | O que demonstrar | Resultado obtido (22/10/2026, H2) |
|---|---|---|---|
| `TEMPO_SECO` | 31 °C, 28 %, 0 mm, 8 km/h, WMO 0 | sem precipitação; "Céu limpo" | ✅ 30.6–31.4 °C, 28 %, `presencaPrecipitacao=false`, "Céu limpo" |
| `CHUVA_FORTE` | 19.5 °C, 96 %, 8.2 mm, 22 km/h, WMO 65 | presença de precipitação = sim; "Chuva" | ✅ 19.1–19.3 °C, 96 %, 8.2 mm, `presencaPrecipitacao=true` |
| `VENTO_FORTE` | 22 °C, 70 %, 0 mm, 68 km/h, WMO 3 | pico no gráfico de vento | ✅ 67–68 km/h |
| `DADOS_INCOMPLETOS` | só temperatura | valores ausentes mostrados como "desconhecido", sem quebrar | ✅ umidade/precipitação/vento `null`, `presencaPrecipitacao=null` |

Outros resultados da mesma execução (coletor REAL e SIMULADO gravando no mesmo banco, webapp consultando):

| Verificação | Resultado obtido |
|---|---|
| `GET /api/leituras?limite=100` | 3 REAL + 9 SIMULADO |
| `GET /api/leituras?modo=SIMULADO` | só as 9 SIMULADO |
| `GET /api/leituras/ultima?modo=REAL` | leitura REAL com `latitude: -23.514938`, `longitude: -46.610504`, `fonteDados: "Open-Meteo"` |
| `GET /api/leituras/ultima?modo=simulado` | leitura SIMULADA com as coordenadas configuradas e `fonteDados: "Simulador"` |
| `GET /api/leituras?modo=TESTE` | 400 "Modo inválido: 'TESTE' (use REAL ou SIMULADO)" |
| período de 40 dias | 400 "O período consultado não pode passar de 31 dias" |

Também é possível inserir os cenários direto no banco com `database/dados-teste.sql` (leituras REAL e SIMULADO de exemplo).
