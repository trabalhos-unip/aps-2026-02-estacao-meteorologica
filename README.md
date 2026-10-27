# aps-2026-02-estacao-meteorologica
APS 2026/2 — Sistema de estação meteorológica para coleta, armazenamento e visualização web de dados ambientais utilizando Java, Hibernate e MySQL.

O sistema tem **duas aplicações** que usam o mesmo banco MySQL:

- **Coletor** (`coletor/`): consulta a [Open-Meteo](https://open-meteo.com/en/docs) a cada 15 minutos e grava cada leitura no banco.
- **Servidor web** (`servidor-web/`): lê as leituras do banco e as entrega ao navegador (página HTML/CSS + API JSON para o gráfico).

O módulo `compartilhado/` tem a entidade Hibernate, o DAO e a configuração de banco usados pelas duas.
Arquitetura, modelo de dados e contrato da API: [`documentacao/arquitetura/backend.md`](documentacao/arquitetura/backend.md).

> A Open-Meteo fornece dados de **modelos meteorológicos**, não medições de sensores físicos do grupo. A resposta
> atual informa uma janela de 15 min (`current.interval = 900`), mas a atualização efetiva depende do modelo de
> previsão selecionado para a localização. Por isso cada leitura guarda o horário da coleta e o horário da fonte separadamente.

## Requisitos

- JDK 21 ou superior
- Maven 3.9+ (ou o Maven embutido do IntelliJ)
- MySQL 8 — pelo Docker (`compose.yaml`) ou instalado localmente

## Configuração

1. Copie `.env.exemplo` para `.env` na raiz do projeto e defina as senhas. O `.env` **não** vai para o GitHub.
2. Suba o MySQL:
   - **Docker:** `docker compose up -d` (cria o banco `estacao_meteorologica` e o usuário do `.env`).
   - **MySQL local:** rode `database/schema.sql` e crie um usuário com acesso ao banco, usando os mesmos dados do `.env`.

As duas aplicações leem o `.env` automaticamente (na pasta atual ou na pasta acima). Variáveis de ambiente
com os mesmos nomes também funcionam. A tabela é criada pelo Hibernate na primeira execução se ainda não existir.

## Executar

**IntelliJ IDEA:** use as configurações de execução **Coletor** e **Servidor Web** (já vêm no projeto, pasta `.run/`).

**Terminal** (na raiz do projeto):

```bash
mvn install -DskipTests
mvn -pl coletor spring-boot:run
mvn -pl servidor-web spring-boot:run
```

- Abra <http://localhost:8080> no navegador.
- Pare o coletor com `Ctrl+C`. Uma coleta a cada 15 min soma 96 chamadas por dia. Para executar o intervalo de 10 s
  citado no enunciado apenas durante um teste controlado, defina `COLETA_INTERVALO_MS=10000` antes de iniciar o coletor.

Para popular o banco sem rodar o coletor: `database/dados-teste.sql`.

## Testes

```bash
mvn test
```

Os testes não precisam de MySQL nem de internet: o DAO é testado com H2 em memória (modo MySQL) e o cliente
da API usa um servidor HTTP local que imita a Open-Meteo, incluindo cenários de erro (HTTP 429/503, JSON inválido, sem conexão).

## Estrutura

```
compartilhado/   model/ (LeituraMeteorologica), dao/ + dao/impl/ (Hibernate), banco.properties
coletor/         client/ (ApiMeteorologicaClient), service/ (ColetaMeteorologicaService)
servidor-web/    controller/, service/ (ConsultaMeteorologicaService), dto/, resources/static/ (front-end)
database/        schema.sql, dados-teste.sql
documentacao/    arquitetura/, evidencias-testes/, monografia/
```
