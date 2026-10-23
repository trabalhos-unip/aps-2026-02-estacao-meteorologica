# aps-2026-02-estacao-meteorologica
APS 2026/2 — Sistema de estação meteorológica para coleta, armazenamento e visualização web de dados ambientais utilizando Java, Hibernate e MySQL.

O sistema tem **duas aplicações distintas** que usam o mesmo banco MySQL:

- **collector** (Aplicação 1): consulta a [Open-Meteo](https://open-meteo.com/en/docs) a cada 10 segundos e grava cada leitura no banco.
- **webapp** (Aplicação 2): lê as leituras do banco e as mostra no navegador (página HTML/CSS + gráfico), por meio do `LeituraMeteorologicaServlet`.

O módulo **compartilhado** contém o que as duas usam: a entidade `LeituraMeteorologica`, o DAO e a conexão
Hibernate (`HibernateUtil` + `hibernate.cfg.xml`).

**Tecnologias:** Java 21 · Spring Boot 3.5 (inicialização, agendamento de 10 s e servidor web) · Hibernate
(`HibernateUtil` + `hibernate.cfg.xml`) · padrão DAO · MySQL 8 · HTML/CSS/JavaScript · Maven.

> A Open-Meteo fornece dados de **modelos meteorológicos** (atualizados a cada 15 min), não medições de sensores
> físicos do grupo. Por isso cada leitura guarda o horário da coleta e o horário da fonte separadamente.

## Documentação

| Documento | Conteúdo |
|---|---|
| [`docs/planejamento.md`](docs/planejamento.md) | planejamento da APS (escopo, cronograma, divisão de tarefas) |
| [`docs/arquitetura.md`](docs/arquitetura.md) | estrutura, fluxo, camadas, **Spring Boot em detalhe**, Hibernate/DAO, erros e contrato JSON |
| [`docs/modelo-dados.md`](docs/modelo-dados.md) | tabela, campos, unidades e decisões do modelo de dados |
| [`docs/roteiro-testes.md`](docs/roteiro-testes.md) | testes automatizados e roteiro de testes manuais |
| [`scripts/iniciar-coletor.md`](scripts/iniciar-coletor.md) | passo a passo para rodar o coletor |
| [`scripts/executar-web.md`](scripts/executar-web.md) | passo a passo para rodar a aplicação Web |

## Requisitos

- JDK 21 ou superior
- Maven 3.9+ (ou o Maven embutido do IntelliJ)
- MySQL 8 — pelo Docker (`compose.yaml`) ou instalado localmente

## Configuração

1. Copie `.env.exemplo` para `.env` na raiz do projeto e defina as senhas. O `.env` **não** vai para o GitHub.
2. Suba o MySQL:
   - **Docker:** `docker compose up -d` (cria o banco `estacao_meteorologica` e o usuário do `.env`).
   - **MySQL local:** rode `database/schema.sql` e crie um usuário com acesso ao banco, usando os mesmos dados do `.env`.

O `HibernateUtil` lê o `hibernate.cfg.xml` e completa URL, usuário e senha com os valores do `.env` (ou de variáveis
de ambiente com os mesmos nomes). A tabela é criada pelo Hibernate na primeira execução, se ainda não existir.

## Executar

**IntelliJ IDEA:** use as configurações de execução **Collector (coletor)** e **Webapp (servidor web)**
(já vêm no projeto, pasta `.run/`) e abra <http://localhost:8080>.

**Terminal** (na raiz do projeto):

```bash
mvn install -DskipTests
mvn -pl collector spring-boot:run
mvn -pl webapp spring-boot:run
```

- Pare o coletor com `Ctrl+C`. Uma coleta a cada 10 s soma 8.640 chamadas por dia, então rode só durante os testes
  (veja os limites de uso da Open-Meteo).
- Para popular o banco sem rodar o coletor: `database/dados-teste.sql`.

Detalhes: [`scripts/iniciar-coletor.md`](scripts/iniciar-coletor.md) e [`scripts/executar-web.md`](scripts/executar-web.md).

## Testes

```bash
mvn test
```

Os testes não precisam de MySQL nem de internet: o DAO é testado com o `HibernateUtil` apontando para H2 em memória
(modo MySQL), o cliente da API usa um servidor HTTP local que imita a Open-Meteo (incluindo HTTP 429/503, JSON
inválido e sem conexão) e o webapp é testado com o Spring Boot e o Tomcat embutido rodando de verdade.
Veja [`docs/roteiro-testes.md`](docs/roteiro-testes.md).

## Estrutura

```
compartilhado/  config/ (HibernateUtil), model/ (LeituraMeteorologica), dao/ + dao/impl/, resources/hibernate.cfg.xml
collector/      app/ (CollectorApplication), client/ (ApiMeteorologicaClient), service/ (ColetaMeteorologicaService), util/ (DataHoraUtil)
webapp/         app/ (WebappApplication), controller/ (LeituraMeteorologicaServlet), service/ (ConsultaMeteorologicaService),
                src/main/webapp/ (index.html, css/style.css, js/dashboard.js, js/grafico.js)
database/       schema.sql, dados-teste.sql
docs/           planejamento.md, arquitetura.md, modelo-dados.md, roteiro-testes.md, evidencias/, monografia/
scripts/        iniciar-coletor.md, executar-web.md
```

Pacote Java: `br.com.aps.meteorologia`. Existe também uma versão **sem Spring Boot** na branch `isaque2`.
