# Back-end — arquitetura e contratos

## Módulos (Maven multi-módulo)

| Módulo | Papel | Pacotes |
|---|---|---|
| `compartilhado` | Modelo, acesso a dados e conexão (`HibernateUtil` + `hibernate.cfg.xml`) usados pelas duas aplicações | `model`, `dao`, `dao.impl`, `config`, `util` |
| `coletor` | **Aplicação 1:** API Open-Meteo → banco, a cada 10 s | `client`, `service` |
| `servidor-web` | **Aplicação 2:** banco → navegador (Servlet com JSON + página em `src/main/webapp`) | `controller`, `service`, `dto` |

Pacote base: `br.edu.unip.aps.meteorologia`.

## Fluxo

```
APLICAÇÃO 1 — COLETOR
Open-Meteo → ApiMeteorologicaClient → ColetaMeteorologicaService → LeituraMeteorologica
          → LeituraMeteorologicaDAO → LeituraMeteorologicaDAOImpl (Hibernate) → MySQL

APLICAÇÃO 2 — WEB
Navegador → LeituraMeteorologicaServlet → ConsultaMeteorologicaService
          → LeituraMeteorologicaDAO → Hibernate → MySQL
```

## Tecnologias e onde aparecem

- **Java 21** + **Maven**.
- **Sem framework** (branch `isaque2`): cada aplicação tem um `main` que cria os objetos e os liga.
- **Hibernate 7.4** (ORM): `hibernate.cfg.xml` + `HibernateUtil` criam a `SessionFactory` única (a URL, o usuário e a
  senha vêm do `.env` pela classe `Configuracao`). Entidade mapeada com anotações JPA; consultas HQL na
  `LeituraMeteorologicaDAOImpl`, que abre a `Session`, controla a `Transaction` e fecha a sessão em cada operação.
- **Agendamento:** `ScheduledExecutorService` do Java, executando a coleta a cada 10 s.
- **Web:** Servlet (`LeituraMeteorologicaServlet`, Jakarta Servlet 6.1) em um **Tomcat 11 embutido**, que também
  entrega os arquivos estáticos (`index.html`, `css/`, `js/`).
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
- **Exceções próprias:** `ApiMeteorologicaException` (falhas recuperáveis da API) e `PersistenciaException` (falhas de banco).
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

Sessões e transações: cada método abre uma `Session` pela `HibernateUtil` e a fecha no `finally`. A gravação usa
`beginTransaction()` → `persist()` → `commit()`, com `rollback()` em caso de erro. Qualquer falha de banco vira
`PersistenciaException`. Se o MySQL estiver fora do ar, a próxima chamada tenta conectar de novo.

## Tratamento de erros

**Coletor:** nenhuma falha recuperável derruba o processo. O erro aparece no log e a próxima coleta segue normalmente.

| Situação | Tratamento |
|---|---|
| Timeout (10 s) / sem rede | `ApiMeteorologicaException` → log `WARN` |
| HTTP 429 (limite da API) | `ApiMeteorologicaException` com mensagem específica → log `WARN` |
| HTTP 4xx/5xx | `ApiMeteorologicaException` com o status → log `WARN` |
| JSON inválido / sem `current` / sem nenhuma variável | `ApiMeteorologicaException` → log `WARN` |
| MySQL fora do ar (na partida ou durante a execução) | `PersistenciaException` → log `SEVERE`; reconecta na coleta seguinte |

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

Os arquivos do front-end (HTML, CSS, JS do gráfico) ficam em `servidor-web/src/main/webapp/`
(`index.html`, `css/`, `js/`) e são servidos pelo próprio servidor web. Os arquivos atuais são provisórios.
