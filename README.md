# aps-2026-02-estacao-meteorologica
APS 2026/2 — Sistema de estação meteorológica para coleta, armazenamento e visualização web de dados ambientais utilizando Java, Hibernate e MySQL.

O sistema tem **duas aplicações** que usam o mesmo banco MySQL:

- **Coletor** (`coletor/`): registra a condição atual, carrega os últimos 15 dias e atualiza a previsão dos próximos 7 dias na [Open-Meteo](https://open-meteo.com/en/docs).
- **Servidor web** (`servidor-web/`): entrega leituras atuais, histórico diário e previsão ao navegador (página HTML/CSS + API JSON).

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
```

Depois, em **dois terminais separados** (ou duas execuções do NetBeans):

```bash
# 1. Coletor: faz a carga inicial e permanece atualizando os dados
mvn -pl coletor spring-boot:run
```

```bash
# 2. Servidor web: deixa o dashboard disponível
mvn -pl servidor-web spring-boot:run
```

- Abra <http://localhost:8080> no navegador. Ao iniciar, o coletor preenche imediatamente o histórico de 15 dias e os 7 dias seguintes; a previsão é renovada a cada 6 h.
- Pare o coletor com `Ctrl+C`. A coleta atual usa intervalo de 15 min por padrão; para demonstração, configure `COLETA_INTERVALO_MS`.

### Dados no banco

`leitura_meteorologica` armazena somente coletas feitas pela estação enquanto ela está ligada. Já `resumo_diario_meteorologico` guarda duas séries separadas: `HISTORICO`, reconstituído da Open-Meteo para preencher os dias anteriores, e `PREVISAO`, que contém hoje e os próximos dias. `resumo_horario_meteorologico` completa as horas sem coleta local; quando há os dois dados para a mesma hora, a página prioriza a leitura local. A interface identifica as origens para não apresentar uma previsão como medição real.

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
