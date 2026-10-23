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
- `Coleta ignorada — falha na API: ...` → problema de rede/API; o coletor tenta de novo no próximo ciclo.
- `Coleta ignorada — falha no banco de dados: ...` → MySQL fora do ar ou `.env` incorreto; o coletor tenta de novo.

## Configurações opcionais (no `.env`)

| Variável | Padrão | Uso |
|---|---|---|
| `OPENMETEO_LATITUDE` / `OPENMETEO_LONGITUDE` | São Paulo | localização monitorada |
| `COLETA_INTERVALO_MS` | `10000` | intervalo entre coletas |

> Uma coleta a cada 10 s soma 8.640 chamadas por dia. Rode o coletor só durante os testes e a demonstração.
