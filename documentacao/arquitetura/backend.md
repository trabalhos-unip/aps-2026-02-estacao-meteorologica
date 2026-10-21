# Back-end — arquitetura e contratos

## Módulos (Maven multi-módulo)

| Módulo | Papel | Pacotes |
|---|---|---|
| `compartilhado` | Modelo e acesso a dados usados pelas duas aplicações, e a configuração de banco (`banco.properties`) | `model`, `dao`, `dao.impl`, `util` |
| `coletor` | **Aplicação 1:** API Open-Meteo → banco, a cada 10 s | `client`, `service` |
| `servidor-web` | **Aplicação 2:** banco → navegador (API JSON + página estática) | `controller`, `service`, `dto` |

Pacote base: `br.edu.unip.aps.meteorologia`.

## Fluxo

```
APLICAÇÃO 1 — COLETOR
Open-Meteo → ApiMeteorologicaClient → ColetaMeteorologicaService → LeituraMeteorologica
          → LeituraMeteorologicaDAO → LeituraMeteorologicaDAOImpl (Hibernate) → MySQL

APLICAÇÃO 2 — WEB
Navegador → LeituraMeteorologicaController → ConsultaMeteorologicaService
          → LeituraMeteorologicaDAO → Hibernate → MySQL
```

## Tecnologias e onde aparecem

- **Java 21** + **Maven**.
- **Spring Boot 3.5**: inicia as aplicações, agenda a coleta (`@Scheduled`), sobe o servidor HTTP e cria a
  `SessionFactory` do Hibernate a partir do `banco.properties` (faz o papel do `HibernateUtil` do planejamento).
- **Hibernate 6** (ORM): mapeamento da entidade `LeituraMeteorologica` com anotações JPA; consultas HQL na
  `LeituraMeteorologicaDAOImpl` usando `org.hibernate.Session`.
- **Padrão DAO**: interface `LeituraMeteorologicaDAO` separada da implementação `LeituraMeteorologicaDAOImpl`.
  Serviços e controller dependem só da interface. Nenhum SQL/Hibernate no controller ou na página.
- **MySQL 8.4** (via `compose.yaml`) + driver `mysql-connector-j`.
- **java.net.http.HttpClient** + **Jackson** para a chamada HTTP e a leitura do JSON da Open-Meteo.
- **JUnit 5, Mockito, AssertJ, H2** nos testes.

## POO aplicada

- **Encapsulamento:** atributos privados na entidade, acesso por getters/setters.
- **Abstração/polimorfismo:** o código usa o tipo `LeituraMeteorologicaDAO`, e a implementação Hibernate é injetada.
  Dá para trocar a implementação (ex.: um DAO falso nos testes) sem mudar quem usa.
- **Responsabilidade única:** cada classe tem uma função (cliente HTTP, regra de coleta, acesso a dados, endpoint).
- **Exceção própria:** `ApiMeteorologicaException` representa falhas recuperáveis da API.
- **Records** (`RespostaOpenMeteo`, `LeituraMeteorologicaDTO`) para objetos imutáveis de transporte de dados.

## Modelo de dados — `leitura_meteorologica`

| Campo Java | Coluna | Origem na Open-Meteo | Unidade |
|---|---|---|---|
| `id` | `id` | — (auto incremento) | — |
| `dataHoraColeta` | `data_hora_coleta` | relógio do coletor no momento da consulta | data/hora (America/Sao_Paulo) |
| `dataHoraFonte` | `data_hora_fonte` | `current.time` | data/hora (America/Sao_Paulo) |
| `temperatura` | `temperatura` | `current.temperature_2m` | °C |
| `umidadeRelativa` | `umidade_relativa` | `current.relative_humidity_2m` | % |
| `precipitacao` | `precipitacao` | `current.precipitation` | mm |
| `chuva` | `chuva` | `current.rain` | mm |
| `velocidadeVento` | `velocidade_vento` | `current.wind_speed_10m` | km/h |
| `codigoClima` | `codigo_clima` | `current.weather_code` | código WMO |
| `presencaPrecipitacao` | `presenca_precipitacao` | **derivado** | booleano |
| `fonteDados` | `fonte_dados` | fixo `"Open-Meteo"` | — |

- **Valores ausentes:** gravados como `NULL` (tipos `Double`/`Integer`, nunca zero no lugar de "sem dado").
- **Regra de presença de precipitação:** `true` se `precipitation > 0` **ou** `rain > 0`; `null` se os dois vierem ausentes.
- **Dois horários:** a Open-Meteo atualiza as condições atuais a cada **15 minutos** (`current.interval = 900`).
  Coletando a cada 10 s, várias linhas seguidas têm o mesmo `data_hora_fonte` e os mesmos valores. Isso é
  esperado: não são novas medições físicas, é o ritmo de execução pedido no enunciado para teste.
- DDL MySQL equivalente: [`database/schema.sql`](../../database/schema.sql).

## DAO — métodos

| Método | Retorno |
|---|---|
| `salvar(LeituraMeteorologica)` | a leitura com `id` preenchido |
| `buscarUltima()` | `Optional` com a leitura mais recente |
| `buscarUltimas(int limite)` | as N mais recentes, **em ordem cronológica** (da mais antiga para a mais recente) |
| `buscarPorPeriodo(LocalDateTime inicio, LocalDateTime fim)` | leituras do intervalo (inclusive), em ordem cronológica |
| `contar()` | total de leituras |

Transações: cada método roda numa transação gerenciada pelo Spring (`@Transactional`), com commit/rollback e
fechamento da sessão automáticos. Erros do Hibernate viram `DataAccessException`.

## Tratamento de erros

**Coletor:** nenhuma falha recuperável derruba o processo. O erro aparece no log e a próxima coleta segue normalmente.

| Situação | Tratamento |
|---|---|
| Timeout (10 s) / sem rede | `ApiMeteorologicaException` → log `WARN` |
| HTTP 429 (limite da API) | `ApiMeteorologicaException` com mensagem específica → log `WARN` |
| HTTP 4xx/5xx | `ApiMeteorologicaException` com o status → log `WARN` |
| JSON inválido / sem `current` / sem nenhuma variável | `ApiMeteorologicaException` → log `WARN` |
| MySQL fora do ar durante a execução | `DataAccessException`/`TransactionException` → log `ERROR` |

Os logs não mostram credenciais.

**Servidor web:** sempre responde JSON no formato `{"status": 400, "erro": "mensagem", "dataHora": "..."}`.

| Situação | HTTP |
|---|---|
| Banco sem leituras (`/ultima`) | 404 `"Nenhuma leitura registrada ainda"` |
| Parâmetro inválido/ausente | 400 |
| MySQL indisponível | 503 `"Banco de dados indisponível no momento"` |

## Contrato com o front-end

Base: `http://localhost:8080`. Datas em ISO 8601 (`2026-10-20T22:20:05`). Campos sem valor vêm como `null`.

| Endpoint | Descrição |
|---|---|
| `GET /api/leituras/ultima` | leitura mais recente (404 se vazio) |
| `GET /api/leituras?limite=100` | últimas N leituras (1–1000, padrão 100), **já em ordem cronológica** para o gráfico; `[]` se vazio |
| `GET /api/leituras/periodo?inicio=2026-10-20T00:00:00&fim=2026-10-20T23:59:59` | leituras do período (máx. 31 dias) |
| `GET /api/leituras/total` | `{"total": 123}` |

Exemplo de leitura (resposta real do teste ponta a ponta):

```json
{
  "id": 12,
  "dataHoraColeta": "2026-10-20T22:39:01.983241",
  "dataHoraFonte": "2026-10-20T22:30:00",
  "temperatura": 14.0,
  "umidadeRelativa": 90,
  "precipitacao": 0.0,
  "chuva": 0.0,
  "velocidadeVento": 9.9,
  "codigoClima": 3,
  "descricaoClima": "Nublado",
  "presencaPrecipitacao": false,
  "fonteDados": "Open-Meteo"
}
```

Os arquivos do front-end (HTML, CSS, JS do gráfico) ficam em `servidor-web/src/main/resources/static/`
(`index.html`, `css/`, `js/`) e são servidos pelo próprio servidor web. O `index.html` atual é provisório.
