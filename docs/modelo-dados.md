# Modelo de dados

Banco MySQL `estacao_meteorologica` (nome definido no `compose.yaml`/`.env.exemplo`), tabela `leitura_meteorologica`.
Entidade Java: `compartilhado/src/main/java/br/com/aps/meteorologia/model/LeituraMeteorologica.java`.
DDL: [`database/schema.sql`](../database/schema.sql) · Dados de teste: [`database/dados-teste.sql`](../database/dados-teste.sql).

## Tabela `leitura_meteorologica`

| Campo Java | Coluna | Tipo MySQL | Origem na Open-Meteo | Unidade |
|---|---|---|---|---|
| `id` | `id` | `BIGINT AUTO_INCREMENT` (PK) | — | — |
| `dataHoraColeta` | `data_hora_coleta` | `DATETIME(6) NOT NULL` | relógio do coletor no momento da consulta | data/hora (America/Sao_Paulo) |
| `dataHoraFonte` | `data_hora_fonte` | `DATETIME(6)` | `current.time` | data/hora (America/Sao_Paulo) |
| `temperatura` | `temperatura` | `DOUBLE` | `current.temperature_2m` | °C |
| `umidadeRelativa` | `umidade_relativa` | `INT` | `current.relative_humidity_2m` | % |
| `precipitacao` | `precipitacao` | `DOUBLE` | `current.precipitation` | mm |
| `chuva` | `chuva` | `DOUBLE` | `current.rain` | mm |
| `velocidadeVento` | `velocidade_vento` | `DOUBLE` | `current.wind_speed_10m` | km/h |
| `codigoClima` | `codigo_clima` | `INT` | `current.weather_code` | código WMO |
| `presencaPrecipitacao` | `presenca_precipitacao` | `BIT(1)` | **derivado** (ver regra) | sim/não |
| `fonteDados` | `fonte_dados` | `VARCHAR(50) NOT NULL` | fixo `"Open-Meteo"` | — |

Índice `idx_leitura_data_hora_coleta` em `data_hora_coleta`, usado nas consultas ordenadas por tempo.

`descricaoClima` (ex.: "Nublado") **não é coluna**: é calculada a partir de `codigoClima` (`@Transient`) e enviada à página.

## Decisões

- **Dois horários:** a Open-Meteo atualiza as condições atuais a cada **15 minutos** (`current.interval = 900`).
  Coletando a cada 10 s, várias linhas seguidas têm o mesmo `data_hora_fonte` e os mesmos valores. Isso é
  esperado: não são novas medições físicas, é o ritmo de execução exigido pelo enunciado para teste.
- **Data/hora:** `LocalDateTime` no fuso `America/Sao_Paulo` (o mesmo enviado à API no parâmetro `timezone`).
  `dataHoraColeta` é gravada com precisão de milissegundos.
- **Unidades:** as padrão da Open-Meteo (°C, %, mm, km/h), sem conversão.
- **Valores ausentes:** gravados como `NULL` (tipos `Double`/`Integer` em Java), nunca zero no lugar de "sem dado".
  Uma resposta sem nenhuma das variáveis principais é rejeitada e não é gravada.
- **Regra de presença de precipitação:** `true` se `precipitation > 0` **ou** `rain > 0`; `false` se ambos forem 0;
  `NULL` (desconhecido) se os dois vierem ausentes.
- **Duplicação:** cada ciclo de coleta grava exatamente uma linha (uma requisição → um `persist`). Linhas
  consecutivas com valores iguais são intencionais (ver "Dois horários").
- **Criação da tabela:** `hibernate.hbm2ddl.auto=update` no `hibernate.cfg.xml` cria/ajusta a tabela automaticamente.
  Depois de aprovado o `schema.sql`, trocar para `validate`.

## Métodos do DAO (`LeituraMeteorologicaDAO`)

| Método | Uso |
|---|---|
| `salvar(LeituraMeteorologica leitura)` | collector grava cada coleta |
| `buscarUltima()` | painel: valores mais recentes |
| `buscarUltimas(int limite)` | gráfico/histórico, em ordem cronológica |
| `buscarPorPeriodo(LocalDateTime dataInicio, LocalDateTime dataFim)` | histórico filtrado por período |
| `contar()` | total de leituras |
