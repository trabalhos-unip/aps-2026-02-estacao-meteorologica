# aps-2026-02-estacao-meteorologica
APS 2026/2 — Sistema de estação meteorológica para coleta, armazenamento e visualização web de dados ambientais utilizando Java, Hibernate e MySQL.

> **Branch `isaque2` — versão sem framework.** Mesmo sistema da branch `isaque`, mas sem Spring Boot:
> Hibernate configurado por `hibernate.cfg.xml` + `HibernateUtil` (como ensinado em aula), DAO abrindo e fechando
> sessão/transação manualmente, coleta agendada com `ScheduledExecutorService` e página/API servidas por Servlet
> em um Tomcat embutido.

O sistema tem **duas aplicações** que usam o mesmo banco MySQL:

- **Coletor** (`coletor/`): consulta a [Open-Meteo](https://open-meteo.com/en/docs) a cada 10 segundos e grava cada leitura no banco.
- **Servidor web** (`servidor-web/`): lê as leituras do banco e as entrega ao navegador (página HTML/CSS + Servlet com JSON para o gráfico).

O módulo `compartilhado/` tem a entidade Hibernate, o DAO, o `HibernateUtil` e a leitura das configurações, usados pelas duas.
Arquitetura, modelo de dados e contrato da API: [`documentacao/arquitetura/backend.md`](documentacao/arquitetura/backend.md).

> A Open-Meteo fornece dados de **modelos meteorológicos** (atualizados a cada 15 min), não medições de sensores
> físicos do grupo. Por isso cada leitura guarda o horário da coleta e o horário da fonte separadamente.

## Requisitos

- JDK 21 ou superior
- Maven 3.9+ (ou o Maven embutido do IntelliJ)
- MySQL 8 — pelo Docker (`compose.yaml`) ou instalado localmente

Não é preciso instalar Tomcat: o servidor web já vem embutido.

## Configuração

1. Copie `.env.exemplo` para `.env` na raiz do projeto e defina as senhas. O `.env` **não** vai para o GitHub.
2. Suba o MySQL:
   - **Docker:** `docker compose up -d` (cria o banco `estacao_meteorologica` e o usuário do `.env`).
   - **MySQL local:** rode `database/schema.sql` e crie um usuário com acesso ao banco, usando os mesmos dados do `.env`.

O `HibernateUtil` lê o `hibernate.cfg.xml` e completa URL, usuário e senha com os valores do `.env`
(ou de variáveis de ambiente com os mesmos nomes). A tabela é criada pelo Hibernate na primeira execução.

## Executar

**IntelliJ IDEA:** use as configurações de execução **Coletor** e **Servidor Web** (já vêm no projeto, pasta `.run/`).
Elas rodam a partir da raiz do projeto.

**Terminal** (na raiz do projeto):

```bash
mvn install -DskipTests
mvn -pl coletor exec:java
mvn -pl servidor-web exec:java
```

- Abra <http://localhost:8080> no navegador.
- Pare o coletor com `Ctrl+C`. Uma coleta a cada 10 s soma 8.640 chamadas por dia, então rode só durante os testes
  (veja os limites de uso da Open-Meteo).

Para popular o banco sem rodar o coletor: `database/dados-teste.sql`.

## Testes

```bash
mvn test
```

Os testes não precisam de MySQL nem de internet: o DAO é testado com o `HibernateUtil` apontando para H2 em memória
(modo MySQL), o cliente da API usa um servidor HTTP local que imita a Open-Meteo (incluindo HTTP 429/503, JSON
inválido e sem conexão) e o Servlet é testado num Tomcat embutido real.

## Estrutura

```
compartilhado/   model/ (LeituraMeteorologica), dao/ + dao/impl/ (Hibernate),
                 config/ (HibernateUtil, Configuracao), resources/hibernate.cfg.xml
coletor/         client/ (ApiMeteorologicaClient), service/ (ColetaMeteorologicaService), ColetorApplication
servidor-web/    controller/ (LeituraMeteorologicaServlet), service/, dto/, ServidorWebApplication,
                 src/main/webapp/ (index.html, css/, js/ — front-end)
database/        schema.sql, dados-teste.sql
documentacao/    arquitetura/, evidencias-testes/, monografia/
```
