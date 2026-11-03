# Estação Meteorológica — APS 2026/2

Sistema acadêmico para coletar, armazenar e visualizar dados meteorológicos. O projeto consulta a
[Open-Meteo](https://open-meteo.com/en/docs), grava leituras, histórico e previsão em um banco MySQL e disponibiliza
uma interface Web responsiva com dados, gráficos e seleção de cidades brasileiras.

> A Open-Meteo fornece dados de modelos meteorológicos, e não medições de sensores físicos instalados pelo grupo.
> Por isso, cada leitura mantém separadamente o horário da coleta e o horário informado pela fonte.

## Componentes do projeto

O projeto Maven possui três módulos:

- **`collector`**: obtém dados meteorológicos reais ou simulados, grava as leituras no MySQL e registra o resultado de
  cada tentativa em `log_coleta`;
- **`webapp`**: oferece a API JSON e a interface Meteora, com visão geral, histórico, previsão horária e diária,
  configurações, temas claro/escuro e seleção da cidade monitorada;
- **`compartilhado`**: contém as entidades, DAOs, regras de configuração e conexão Hibernate utilizadas pelas duas
  aplicações.

O banco possui cinco tabelas principais:

- `estacao`: cadastro da estação monitorada;
- `leitura_meteorologica`: dados meteorológicos coletados;
- `log_coleta`: histórico de sucessos e falhas do coletor;
- `resumo_diario_meteorologico`: histórico diário reconstituído e previsão diária, separados por tipo;
- `resumo_horario_meteorologico`: série horária que preenche lacunas quando o coletor ficou desligado e armazena a
  probabilidade de chuva por horário.

A instalação inicial utiliza **Sorocaba/SP**. Pela tela de configurações, é possível escolher qualquer cidade do
Brasil informando cidade e UF. A aplicação consulta a Geocoding API da Open-Meteo, persiste cidade, UF, coordenadas e
fuso no MySQL e marca essa estação como ativa. Leituras e resumos são isolados por estação; o collector usa as
coordenadas da estação ativa no próximo ciclo, sem precisar ser reiniciado.

## Organização e princípios SOLID

- interfaces começam com `I`, como `IFonteMeteorologica` e `ILeituraMeteorologicaDAO`;
- serviços dependem de interfaces, e não das implementações Hibernate ou Open-Meteo;
- `ClienteOpenMeteo` e `SimuladorMeteorologico` implementam o mesmo contrato e entregam
  `DadosMeteorologicosAtuais` em um formato independente da fonte;
- `ConversorLeituraMeteorologica` possui somente a responsabilidade de transformar os dados recebidos em entidade;
- implementações de persistência possuem nomes explícitos, como `LeituraMeteorologicaDAOHibernate`;
- `OperacaoHibernate` centraliza abertura de sessão, transação, commit, rollback e conversão de erros.

## Tecnologias

- Java 21;
- Spring Boot 3.5.6;
- Hibernate ORM e padrão DAO;
- MySQL 8;
- Maven;
- HTML, CSS e JavaScript;
- Tomcat embutido no Spring Boot;
- Docker e Caddy no ambiente de publicação da Oracle Cloud.

## Requisitos

- JDK 21 ou superior;
- Maven 3.9 ou o Maven integrado ao IntelliJ IDEA;
- MySQL 8 instalado e em execução;
- acesso à internet para coletas no modo `REAL`, sincronização de histórico/previsão e busca de cidades.

Não é necessário instalar um servidor Tomcat separadamente.

## Configuração do ambiente

### 1. Criar o arquivo `.env`

Na raiz do projeto, copie `.env.exemplo` para `.env`.

No PowerShell:

```powershell
Copy-Item .env.exemplo .env
```

Preencha no `.env` os dados correspondentes ao MySQL da sua máquina:

```dotenv
MYSQL_HOST=localhost
MYSQL_PORT=3306
MYSQL_DATABASE=estacao_meteorologica
MYSQL_USER=aps
MYSQL_PASSWORD=
```

Preencha `MYSQL_PASSWORD` depois do sinal `=` com a senha definida para o usuário `aps` na sua máquina. Esse é o
usuário da aplicação e deve possuir acesso ao banco `estacao_meteorologica`. Não é recomendado executar a aplicação
utilizando o usuário administrativo `root`.

O projeto procura as configurações nesta ordem:

1. propriedades Java informadas com `-D`;
2. variáveis de ambiente do sistema;
3. arquivo `.env`;
4. valores padrão definidos no código.

### 2. Preparar o banco de dados

Para uma instalação nova, conecte-se ao MySQL com um usuário administrativo e execute
[`database/schema.sql`](database/schema.sql). O script:

- cria o banco `estacao_meteorologica`;
- cria as tabelas `estacao`, `leitura_meteorologica`, `log_coleta`, `resumo_diario_meteorologico` e `resumo_horario_meteorologico`;
- adiciona índices, relacionamentos e validações;
- cadastra a estação inicial de Sorocaba.

O usuário e a senha definidos no `.env` devem existir no MySQL e possuir acesso a esse banco.

O Hibernate utiliza `hibernate.hbm2ddl.auto=validate`: ele apenas verifica se o banco é compatível com as entidades
Java. Portanto, **o Hibernate não cria nem atualiza as tabelas automaticamente**.

Para atualizar uma instalação anterior à V2, faça um backup e execute a migration consolidada
[`database/migrations/V2__estacao_log_e_variaveis_extras.sql`](database/migrations/V2__estacao_log_e_variaveis_extras.sql).
Ela preserva as leituras e resumos existentes e acrescenta estação, localização brasileira, logs,
resumos por estação e probabilidades de chuva.
Não execute a migration depois do `schema.sql` atual, pois uma instalação nova já nasce completa. Bancos que já
receberam a sequência antiga de migrations V2–V6 também não devem reaplicar a V2 consolidada.

O arquivo [`database/dados-teste.sql`](database/dados-teste.sql) é opcional e insere leituras reais e simuladas para
testes da interface.

## Compilar o projeto

Na primeira execução, ou depois de alterações no módulo `compartilhado`, execute na raiz do projeto:

```powershell
mvn install -DskipTests
```

## Executar as aplicações

O MySQL deve estar funcionando antes de iniciar as aplicações.

### Pelo IntelliJ IDEA

O projeto possui configurações prontas na pasta `.run`:

1. execute **Collector (coletor)**;
2. execute **Webapp (servidor web)**;
3. abra <http://localhost:8080>.

As duas configurações podem permanecer em execução ao mesmo tempo.

### Pelo terminal

Abra dois terminais na raiz do projeto.

No primeiro terminal, inicie o collector:

```powershell
mvn -pl collector spring-boot:run
```

No segundo terminal, inicie o webapp:

```powershell
mvn -pl webapp spring-boot:run
```

Para interromper uma aplicação iniciada pelo terminal, pressione `Ctrl+C`.

### Trocar de cidade

Com o **webapp** e o **collector** em execução, abra **Configurações**, informe a cidade e selecione a UF. O país é
fixado em Brasil. Ao salvar, o webapp obtém as coordenadas na Open-Meteo, cria ou atualiza a estação no banco e a
ativa. A próxima coleta já usa a nova posição. Os dados históricos da cidade anterior continuam guardados, mas não
são exibidos enquanto outra estação estiver ativa. Em instantes, o collector também carrega os 30 dias
históricos e os 15 dias de previsão da nova cidade.

## Modos de coleta

### REAL

É o modo padrão. O collector consulta a Open-Meteo e salva as leituras com:

- `modo_operacao = REAL`;
- `fonte_dados = Open-Meteo`.

Se `COLETA_MODO` não estiver definido no `.env`, o modo `REAL` será utilizado automaticamente.

### SIMULADO

Gera dados de teste sem consultar a internet. Para ativá-lo, adicione ao `.env`:

```dotenv
COLETA_MODO=SIMULADO
```

Sem `COLETA_CENARIO`, o sistema alterna entre `TEMPO_SECO`, `CHUVA_FORTE`, `VENTO_FORTE` e
`DADOS_INCOMPLETOS`. Para manter um único cenário, configure, por exemplo:

```dotenv
COLETA_CENARIO=CHUVA_FORTE
```

As leituras simuladas são salvas com `modo_operacao = SIMULADO` e `fonte_dados = Simulador`. Reinicie o collector
sempre que alterar o modo ou o cenário.

## Configurações disponíveis

| Variável | Padrão | Finalidade |
|---|---|---|
| `MYSQL_HOST` | `localhost` | Endereço do MySQL |
| `MYSQL_PORT` | `3306` | Porta do MySQL |
| `MYSQL_DATABASE` | `estacao_meteorologica` | Nome do banco |
| `MYSQL_USER` | `aps` | Usuário da aplicação |
| `MYSQL_PASSWORD` | vazio | Senha local do usuário da aplicação |
| `ESTACAO_NOME` | `Estacao Sorocaba` | Nome alternativo usado somente antes do primeiro cadastro no banco |
| `OPENMETEO_LATITUDE` | `-23.5015` | Coordenada alternativa antes do primeiro cadastro ou no modo simulado |
| `OPENMETEO_LONGITUDE` | `-47.4526` | Coordenada alternativa antes do primeiro cadastro ou no modo simulado |
| `OPENMETEO_TIMEZONE` | `America/Sao_Paulo` | Fuso padrão do webapp; a estação ativa mantém seu próprio fuso no banco |
| `COLETA_INTERVALO_MS` | `10000` | Intervalo entre coletas em milissegundos |
| `COLETA_VERIFICACAO_ESTACAO_MS` | `1000` | Verificação leve de troca de estação; consulta a API somente quando há mudança |
| `SINCRONIZACAO_HISTORICO_DIAS` | `30` | Quantidade de dias anteriores carregados da Open-Meteo |
| `SINCRONIZACAO_PREVISAO_DIAS` | `15` | Quantidade de dias futuros carregados da Open-Meteo |
| `SINCRONIZACAO_INTERVALO_MS` | `21600000` | Atualização periódica do histórico e da previsão (6 horas) |
| `SINCRONIZACAO_VERIFICACAO_ESTACAO_MS` | `1000` | Verificação de troca de cidade para recarregar histórico e previsão |
| `COLETA_MODO` | `REAL` | Origem da coleta: `REAL` ou `SIMULADO` |
| `COLETA_CENARIO` | vazio | Cenário fixo no modo simulado; vazio alterna os cenários |
| `SERVIDOR_PORTA` | `8080` | Porta HTTP do webapp |

Uma coleta a cada 10 segundos pode gerar até 8.640 chamadas por dia. Utilize o modo `REAL` apenas durante os períodos
necessários de teste e demonstração, respeitando os limites da Open-Meteo.

## Endereços do webapp

| Endereço | Resultado |
|---|---|
| <http://localhost:8080> | Visão geral com condições atuais, gráficos e previsões |
| <http://localhost:8080/historico> | Histórico por período e variável meteorológica |
| <http://localhost:8080/configuracoes> | Localização, modo visual e preferências da interface |
| <http://localhost:8080/api/leituras/ultima> | Leitura mais recente em JSON |
| <http://localhost:8080/api/leituras?limite=20> | Últimas 20 leituras em ordem cronológica |
| <http://localhost:8080/api/leituras?modo=REAL> | Somente leituras reais |
| <http://localhost:8080/api/leituras?modo=SIMULADO> | Somente leituras simuladas |
| <http://localhost:8080/api/leituras/total> | Quantidade total de leituras no banco |
| <http://localhost:8080/api/estacao> | Estação brasileira ativa |
| <http://localhost:8080/api/estacao/sugestoes?termo=Sorocaba> | Sugestões de cidades brasileiras |
| <http://localhost:8080/api/fonte> | Fonte e localização atualmente consultadas |
| <http://localhost:8080/api/resumos-diarios/previsoes?dias=5> | Previsão diária, de 1 a 16 dias |

A consulta por período também está disponível:

```text
http://localhost:8080/api/leituras/periodo?inicio=2026-10-20T00:00:00&fim=2026-10-20T23:59:59
```

O período máximo é de 31 dias e o limite máximo é de 1.000 registros. Os parâmetros `modo` e `limite` são opcionais.

Também existem consultas de resumos diários e horários:

```text
GET /api/resumos-diarios/historico?inicio=2026-10-01&fim=2026-10-30
GET /api/resumos-diarios/previsoes?dias=5
GET /api/resumos-horarios/historico?inicio=2026-10-30T00:00:00&fim=2026-10-30T23:59:59
GET /api/resumos-horarios/previsoes?inicio=2026-10-31T00:00:00&fim=2026-11-01T23:59:59
```

Para selecionar outra cidade, envie `POST /api/estacao` com JSON no formato
`{"cidade":"Campinas","estado":"SP"}`. A estação anterior e seus dados permanecem preservados.

## Testes

Execute todos os testes na raiz do projeto:

```powershell
mvn test
```

Estado verificado em 02/11/2026: **58 testes, 0 falhas, 0 erros e 0 ignorados** — 11 no módulo `compartilhado`,
30 no `collector` e 17 no `webapp`.

A suíte conclui sem exigir um MySQL disponível nem acesso externo à Open-Meteo. O módulo compartilhado usa H2 em
memória no modo de compatibilidade com MySQL, e o cliente da API é testado com um servidor HTTP local. Durante os
testes de contexto do collector podem aparecer tentativas de sincronização com o MySQL local; a indisponibilidade é
tratada e não interrompe a suíte.

## Segurança e arquivos locais

- nunca envie o `.env`, senhas reais ou credenciais para o Git;
- não versione dumps do banco contendo dados ou credenciais locais;
- mantenha somente exemplos sem segredos no `.env.exemplo`;
- confirme os arquivos modificados com `git status` antes de criar um commit.

## Estrutura de pastas

```text
compartilhado/  Entidades, interfaces de persistência, DAOs Hibernate e configurações compartilhadas
collector/      Fontes meteorológicas, normalização dos dados, conversor e serviço de coleta
webapp/         Servlet, serviço de consulta e interface HTML/CSS/JavaScript
database/       Schema atual, migração da estrutura antiga e dados de teste
deploy/oracle/  Dockerfiles, Compose, Caddy e guia de publicação na Oracle Cloud
docs/           Arquitetura, modelo de dados, planejamento, testes e evidências
scripts/        Guias detalhados para executar o collector e o webapp
.run/           Configurações de execução do IntelliJ IDEA
```

Pacote Java principal: `br.com.aps.meteorologia`.

## Documentação complementar

Para publicar a aplicação completa em uma VM Oracle Cloud Always Free, consulte
[`deploy/oracle/GUIA.md`](deploy/oracle/GUIA.md). Ele inclui os Dockerfiles do
`webapp` e do `collector`, MySQL persistente, configuração para VMs de 1 GB, Caddy com HTTPS e acesso administrativo ao
banco por túnel SSH. O workflow de GitHub Actions só executa quando `ORACLE_DEPLOY_ENABLED=true` e está atualmente
associado à branch `feature/probabilidade-chuva-horaria`.

| Documento | Conteúdo |
|---|---|
| [`docs/planejamento.md`](docs/planejamento.md) | Escopo, cronograma e divisão de tarefas |
| [`docs/arquitetura.md`](docs/arquitetura.md) | Fluxo, camadas, Spring Boot, Hibernate, DAO e contrato JSON |
| [`docs/modelo-dados.md`](docs/modelo-dados.md) | Tabelas, relacionamentos, campos e regras do banco |
| [`docs/roteiro-testes.md`](docs/roteiro-testes.md) | Testes automatizados e roteiro de testes manuais |
| [`scripts/iniciar-coletor.md`](scripts/iniciar-coletor.md) | Instruções detalhadas do collector |
| [`scripts/executar-web.md`](scripts/executar-web.md) | Instruções detalhadas do webapp |
