# Iniciar o coletor (collector)

Aplicação 1: consulta a Open-Meteo a cada 10 segundos e grava cada leitura no MySQL.

## Antes de começar

1. JDK 21+ e Maven instalados (ou o Maven do IntelliJ).
2. Arquivo `.env` na raiz do projeto, copiado de `.env.exemplo`, com as senhas definidas.
3. MySQL rodando:
   - Docker: `docker compose up -d` (na raiz do projeto).
   - Ou MySQL local com o banco `estacao_meteorologica` e o usuário do `.env` (script: `database/schema.sql`).

## Pelo IntelliJ IDEA

1. Abra a pasta do projeto e espere o Maven carregar.
2. No seletor de execução (canto superior direito), escolha **Collector (coletor)** e clique em ▶.
3. Para parar: botão ■ (Stop).

## Pelo terminal (na raiz do projeto)

```bash
mvn install -DskipTests
mvn -pl collector spring-boot:run
```

Para parar: `Ctrl+C`.

## O que deve aparecer no log

```text
... Leitura #1 salva | coleta=2026-10-22T14:00:00.123 fonte=2026-10-22T14:00 | 22.4 °C, 61 %, precip. 0.0 mm, chuva 0.0 mm, vento 11.2 km/h, código WMO 2
... Leitura #2 salva | coleta=2026-10-22T14:00:10.245 fonte=2026-10-22T14:00 | ...
```

- Uma linha a cada ~10 segundos. Valores repetidos são normais: a Open-Meteo atualiza a cada 15 minutos.
- `[REAL]` ou `[SIMULADO]` mostra o modo de operação da leitura.
- `Coleta ignorada — falha na fonte de dados (Open-Meteo): ...` → problema de rede/API; o coletor tenta de novo no próximo ciclo.
- `Coleta ignorada — falha no banco de dados: ...` → MySQL fora do ar ou `.env` incorreto; o coletor tenta de novo.

## Modo simulado (cenários de teste)

Para demonstrar condições meteorológicas diferentes sem depender do tempo real (e sem acessar a internet),
coloque no `.env`:

```text
COLETA_MODO=SIMULADO
```

Sem `COLETA_CENARIO`, o coletor alterna a cada coleta entre os 4 cenários:

| Cenário | O que gera |
|---|---|
| `TEMPO_SECO` | ~31 °C, 28 %, sem chuva, vento fraco |
| `CHUVA_FORTE` | ~19.5 °C, 96 %, 8.2 mm, presença de precipitação = sim |
| `VENTO_FORTE` | ~22 °C, 70 %, sem chuva, vento ~68 km/h |
| `DADOS_INCOMPLETOS` | só temperatura; o resto ausente (testa "desconhecido" na página) |

Para fixar um cenário: `COLETA_CENARIO=CHUVA_FORTE` (por exemplo). As leituras simuladas são gravadas com
`modoOperacao = SIMULADO` e `fonteDados = "Simulador"` e podem ser separadas na página com `?modo=SIMULADO`.
Para voltar à coleta real, apague as linhas ou use `COLETA_MODO=REAL`.

## Configurações opcionais (no `.env`)

| Variável | Padrão | Uso |
|---|---|---|
| `OPENMETEO_LATITUDE` / `OPENMETEO_LONGITUDE` | São Paulo | localização monitorada |
| `COLETA_INTERVALO_MS` | `10000` | intervalo entre coletas |
| `COLETA_MODO` | `REAL` | `REAL` (Open-Meteo) ou `SIMULADO` (cenários de teste) |
| `COLETA_CENARIO` | vazio (alterna) | só no modo SIMULADO: `TEMPO_SECO`, `CHUVA_FORTE`, `VENTO_FORTE`, `DADOS_INCOMPLETOS` |

> Uma coleta a cada 10 s soma 8.640 chamadas por dia. Rode o coletor só durante os testes e a demonstração.
