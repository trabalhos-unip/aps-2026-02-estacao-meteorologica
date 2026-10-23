# Roteiro de testes

Para cada teste: entrada/procedimento, resultado esperado, resultado obtido, data e evidência.
Evidências (capturas de tela, trechos de log, consultas ao banco) ficam em [`docs/evidencias/`](evidencias/).
Regra do planejamento: **não marcar um teste como executado antes de realizá-lo**.

## 1. Testes automatizados (`mvn test`)

Rodam sem MySQL e sem internet. Resultado de 22/10/2026: **34 testes, 0 falhas**.

| Módulo | Classe de teste | O que verifica |
|---|---|---|
| compartilhado | `LeituraMeteorologicaDAOImplTest` (6) | DAO real com `HibernateUtil` + H2 (modo MySQL): salvar e recuperar, banco vazio, valores ausentes (`NULL`), últimas leituras em ordem cronológica, consulta por período, validação de parâmetros |
| collector | `ApiMeteorologicaClientTest` (7) | cliente HTTP contra servidor local que imita a Open-Meteo: resposta real convertida, URL com todas as variáveis, HTTP 429 (limite), HTTP 503 (indisponível), JSON inválido, resposta sem `current`, sem conexão |
| collector | `ColetaMeteorologicaServiceTest` (7) | conversão API → entidade, valores ausentes, resposta sem variáveis rejeitada, regra de presença de precipitação, gravação pelo DAO, falha da API e falha do banco **não derrubam** o coletor |
| collector | `DataHoraUtilTest` (4) | conversão do `current.time`, horário vazio, formato inválido |
| collector | `CollectorApplicationTest` (1) | Spring Boot do coletor sobe e liga client, service e DAO |
| webapp | `LeituraMeteorologicaServletTest` (9) | Spring Boot + Tomcat embutido reais: última leitura, banco vazio (404), últimas N, limite inválido (400), período, data inválida/ausente (400), banco indisponível (503), total, entrega de `index.html`, `css` e `js` |

## 2. Testes manuais — estratégia mínima do planejamento

| # | Teste | Procedimento | Resultado esperado | Resultado obtido | Data | Evidência |
|---|---|---|---|---|---|---|
| 1 | Open-Meteo disponível | Chamar a Forecast API com as coordenadas de São Paulo e as 6 variáveis | JSON válido; campos e unidades identificados | ✅ JSON com `temperature_2m` (°C), `relative_humidity_2m` (%), `precipitation`/`rain` (mm), `wind_speed_10m` (km/h), `weather_code` e `interval: 900` | 20/10/2026 | — |
| 2 | Persistência | Coletar uma leitura e consultar o banco | Registro salvo e recuperável | ✅ com **H2 no lugar do MySQL**: leitura gravada pelo collector e lida pelo webapp. ⏳ Repetir com MySQL | 22/10/2026 | — |
| 3 | Periodicidade | Executar o coletor por alguns minutos | Coletas a cada ~10 s | ✅ (curto, com H2): coletas às 23:04:58, 23:05:11, 23:05:21, 23:05:32. ⏳ Repetir por 2+ min com MySQL | 22/10/2026 | — |
| 4 | Consulta histórica | Buscar várias leituras por data/hora | Ordem temporal definida | ✅ teste automatizado do DAO e `GET /api/leituras?limite=2` → ids 3, 4 (ordem cronológica) | 22/10/2026 | — |
| 5 | Gráfico | Abrir a página com registros existentes | Gráfico com eixos/unidades legíveis | ⏳ Pendente: `grafico.js` ainda não implementado (front-end). A página já recebe os dados (4 leituras) | — | — |
| 6 | Banco vazio | Abrir a página antes de haver leituras | Mensagem informativa; página não quebra | ✅ no back-end: `/api/leituras/ultima` → 404 "Nenhuma leitura registrada ainda" (teste automatizado). ⏳ Conferir a interface final | 22/10/2026 | — |
| 7 | Falha da API | Simular desconexão ou resposta inválida | Erro tratado e registrado; sistema não encerra | ✅ testes automatizados (429, 503, JSON inválido, sem conexão) | 22/10/2026 | — |
| 7b | Falha do banco | Iniciar o coletor com o MySQL desligado | Erro registrado; coletor continua e reconecta | ✅ na versão sem Spring (mesmo `HibernateUtil`/DAO): erro registrado a cada ciclo, sem encerrar. ⏳ Repetir nesta versão | 21/10/2026 | — |
| 8 | Integração completa | Iniciar banco, coletor e web e abrir a página | Dados aparecem na interface/gráfico | ✅ com H2: API → collector → banco → Servlet → página (última leitura exibida no navegador). ⏳ Repetir com MySQL e gráfico | 22/10/2026 | — |
| 9 | Execução limpa | Seguir só o README num ambiente do zero | Outro integrante inicia sem ajuda | ⏳ Pendente | — | — |

✅ executado · ⏳ pendente

## 3. Cenários para simular condições meteorológicas (monografia, item "e" do enunciado)

Use `database/dados-teste.sql` para inserir leituras com condições diferentes (tempo seco, início de chuva, chuva
moderada e leitura com valores ausentes) e conferir como a página e o gráfico exibem cada caso.
