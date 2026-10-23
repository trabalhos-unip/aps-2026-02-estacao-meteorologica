# Arquitetura do sistema

Sistema de estação meteorológica com **duas aplicações Java distintas** que usam o mesmo banco MySQL:

- **collector** (Aplicação 1): consulta a Open-Meteo a cada 10 segundos e grava cada leitura no banco.
- **webapp** (Aplicação 2): lê as leituras do banco e as entrega ao navegador (página HTML/CSS + gráfico).

Modelo de dados: [`modelo-dados.md`](modelo-dados.md) · Testes: [`roteiro-testes.md`](roteiro-testes.md) ·
Planejamento: [`planejamento.md`](planejamento.md)

---

## 1. Estrutura de pastas

Segue a estrutura do planejamento (seção 4), com uma única diferença: o `model/`, o `dao/` e o `config/`
(HibernateUtil + hibernate.cfg.xml) ficam **uma vez só** no módulo `compartilhado/`, usado pelas duas aplicações,
em vez de copiados dentro de `collector/` e `webapp/`. Assim, uma mudança na tabela é feita em um único lugar
("compartilhar entre as aplicações a mesma configuração de banco, sem duplicar regras de negócio").

```text
aps-2026-02-estacao-meteorologica/
├── README.md
├── .gitignore
├── .env.exemplo                      # modelo das configurações locais (o .env real não vai para o GitHub)
├── compose.yaml                      # MySQL via Docker
├── pom.xml                           # projeto Maven com os 3 módulos
├── docs/
│   ├── planejamento.md
│   ├── modelo-dados.md
│   ├── arquitetura.md
│   ├── roteiro-testes.md
│   ├── evidencias/
│   └── monografia/
│       └── referencias.md            # (monografia.docx é adicionado pela equipe da monografia)
├── database/
│   ├── schema.sql
│   └── dados-teste.sql
├── compartilhado/                    # usado pelo collector e pelo webapp
│   ├── pom.xml
│   └── src/
│       ├── main/
│       │   ├── java/br/com/aps/meteorologia/
│       │   │   ├── config/
│       │   │   │   ├── HibernateUtil.java
│       │   │   │   └── Configuracao.java          # lê o .env (usuário/senha do banco)
│       │   │   ├── model/
│       │   │   │   └── LeituraMeteorologica.java
│       │   │   ├── dao/
│       │   │   │   ├── LeituraMeteorologicaDAO.java
│       │   │   │   ├── PersistenciaException.java
│       │   │   │   └── impl/
│       │   │   │       └── LeituraMeteorologicaDAOImpl.java
│       │   │   └── util/
│       │   │       └── CodigoClimaWMO.java        # código WMO → descrição em português
│       │   └── resources/
│       │       └── hibernate.cfg.xml
│       └── test/
├── collector/                        # Aplicação 1: API → banco
│   ├── pom.xml
│   └── src/
│       ├── main/
│       │   ├── java/br/com/aps/meteorologia/
│       │   │   ├── app/
│       │   │   │   └── CollectorApplication.java
│       │   │   ├── service/
│       │   │   │   └── ColetaMeteorologicaService.java
│       │   │   ├── client/
│       │   │   │   ├── ApiMeteorologicaClient.java
│       │   │   │   ├── ApiMeteorologicaException.java
│       │   │   │   └── RespostaOpenMeteo.java     # formato do JSON da Open-Meteo
│       │   │   └── util/
│       │   │       └── DataHoraUtil.java
│       │   └── resources/
│       │       └── application.properties         # coordenadas e intervalo de coleta
│       └── test/
├── webapp/                           # Aplicação 2: banco → navegador
│   ├── pom.xml
│   └── src/
│       ├── main/
│       │   ├── java/br/com/aps/meteorologia/
│       │   │   ├── app/
│       │   │   │   └── WebappApplication.java
│       │   │   ├── service/
│       │   │   │   └── ConsultaMeteorologicaService.java
│       │   │   └── controller/
│       │   │       └── LeituraMeteorologicaServlet.java
│       │   ├── resources/
│       │   │   └── application.properties         # porta do servidor
│       │   └── webapp/
│       │       ├── index.html
│       │       ├── css/
│       │       │   └── style.css
│       │       └── js/
│       │           ├── dashboard.js
│       │           └── grafico.js
│       └── test/
└── scripts/
    ├── iniciar-coletor.md
    └── executar-web.md
```

`WEB-INF/web.xml` não existe porque, com Spring Boot, o Servlet é registrado em Java
(`WebappApplication.leituraMeteorologicaServlet()`). O planejamento o marca como "se necessário à tecnologia escolhida".

## 2. Fluxo

```text
APLICAÇÃO 1 — COLETOR (collector)
API Open-Meteo
       ↓  HTTP GET (java.net.http.HttpClient)
ApiMeteorologicaClient          → valida status HTTP e JSON
       ↓
ColetaMeteorologicaService      → converte, calcula presença de precipitação  (@Scheduled a cada 10 s)
       ↓
LeituraMeteorologica (model)
       ↓
LeituraMeteorologicaDAO         → LeituraMeteorologicaDAOImpl
       ↓
Hibernate (HibernateUtil + hibernate.cfg.xml)
       ↓
MySQL

APLICAÇÃO 2 — WEB (webapp)
Navegador → LeituraMeteorologicaServlet → ConsultaMeteorologicaService → DAO → Hibernate → MySQL
     ↑                                                                        ↓
     └──────── index.html + css + js (dashboard.js, grafico.js) ← JSON ───────┘
```

## 3. Responsabilidade de cada camada

| Pasta | Classe(s) | Responsabilidade |
|---|---|---|
| `app/` | `CollectorApplication`, `WebappApplication` | Ponto de partida (`main`). Inicia o Spring Boot e entrega ao Spring o DAO e o Servlet. |
| `config/` | `HibernateUtil`, `Configuracao` | Cria a `SessionFactory` única do Hibernate a partir do `hibernate.cfg.xml`, completando URL/usuário/senha com o `.env`. |
| `model/` | `LeituraMeteorologica` | Uma leitura meteorológica como objeto Java, mapeada para a tabela `leitura_meteorologica`. |
| `dao/` | `LeituraMeteorologicaDAO` | Contrato (interface) de acesso aos dados. |
| `dao/impl/` | `LeituraMeteorologicaDAOImpl` | Implementação com Hibernate: abre a sessão, controla a transação, fecha a sessão. Todo acesso ao banco está aqui. |
| `client/` | `ApiMeteorologicaClient` | Comunicação HTTP com a Open-Meteo e leitura do JSON. |
| `service/` | `ColetaMeteorologicaService`, `ConsultaMeteorologicaService` | Regras dos casos de uso: obter/converter/salvar e validar/consultar. |
| `controller/` | `LeituraMeteorologicaServlet` | Recebe a requisição Web, chama o serviço e devolve JSON. Sem SQL nem Hibernate. |
| `util/` | `DataHoraUtil`, `CodigoClimaWMO` | Funções auxiliares de data/hora e tradução do código do clima. |
| `resources/` | `hibernate.cfg.xml`, `application.properties` | Configurações. Nenhuma senha. |
| `webapp/` | `index.html`, `css/`, `js/` | Página, estilo e lógica do painel e do gráfico. |

---

## 4. Spring Boot — o que é e o que faz neste projeto

### 4.1 O que é

**Spring Boot** é um framework Java que já traz prontas várias partes que, sem ele, precisariam ser programadas
à mão: iniciar a aplicação, ler arquivos de configuração, criar os objetos e ligá-los entre si, agendar tarefas
periódicas e subir um servidor web. Ele é usado pelas duas aplicações (versão 3.5.6, definida no `pom.xml` raiz).

O enunciado exige **Java, Hibernate, Web, orientação a objetos e DAO** e não determina framework. O Spring Boot não
substitui nenhum desses itens. O Hibernate e o DAO continuam explícitos no código, e o Spring cuida apenas da
"infraestrutura" das aplicações.

### 4.2 O que o Spring Boot faz — e o que ele NÃO faz

| Tarefa | Quem faz | Onde está no código |
|---|---|---|
| Iniciar a aplicação | Spring Boot | `SpringApplication.run(...)` nos `main` de `app/` |
| Criar os objetos e ligá-los (client → service → DAO) | Spring Boot (injeção de dependência) | `@Service`, `@Component`, `@Bean`, construtores |
| Ler configurações (coordenadas, intervalo, porta) | Spring Boot | `application.properties` + `.env`, `@Value` |
| Executar a coleta a cada 10 segundos | Spring Boot (agendador) | `@EnableScheduling` + `@Scheduled(fixedDelayString = "${coleta.intervalo-ms}")` |
| Servidor web (Tomcat embutido) | Spring Boot | `spring-boot-starter-web` |
| Registrar o Servlet em `/api/leituras/*` | Spring Boot | `ServletRegistrationBean` em `WebappApplication` |
| Entregar `index.html`, `css/`, `js/` | Spring Boot | pasta `webapp/src/main/webapp` (copiada para `static` no build) |
| Converter objetos Java em JSON | Jackson (configurado pelo Spring) | `ObjectMapper` usado pelo Servlet |
| **Conectar ao banco** | **Hibernate (HibernateUtil)** | `config/HibernateUtil.java` + `hibernate.cfg.xml` |
| **Abrir/fechar sessão, commit/rollback** | **DAO (código nosso)** | `dao/impl/LeituraMeteorologicaDAOImpl.java` |
| **Consultas (HQL)** | **DAO (código nosso)** | `LeituraMeteorologicaDAOImpl` |

Ou seja: **o Spring Boot não toca no banco**. Não usamos Spring Data JPA, `@Transactional` nem `@Repository`.
A persistência é feita do jeito ensinado em aula (HibernateUtil + Session + Transaction).

### 4.3 Como cada aplicação inicia

**collector — `CollectorApplication`**

```java
@SpringBootApplication(scanBasePackages = "br.com.aps.meteorologia")
@EnableScheduling
public class CollectorApplication {
    public static void main(String[] args) { SpringApplication.run(CollectorApplication.class, args); }

    @Bean
    public LeituraMeteorologicaDAO leituraMeteorologicaDAO() { return new LeituraMeteorologicaDAOImpl(); }

    @PreDestroy
    public void encerrar() { HibernateUtil.fechar(); }
}
```

1. `@SpringBootApplication` liga a configuração automática e procura classes anotadas em
   `br.com.aps.meteorologia` e subpacotes (`scanBasePackages`), encontrando `ApiMeteorologicaClient` (`@Component`)
   e `ColetaMeteorologicaService` (`@Service`).
2. O método `@Bean` entrega ao Spring o DAO (`new LeituraMeteorologicaDAOImpl()`).
3. O Spring cria o `ApiMeteorologicaClient` preenchendo URL, latitude, longitude e timeout com `@Value`
   (valores do `application.properties`/`.env`).
4. O Spring cria o `ColetaMeteorologicaService`, passando o client e o DAO no construtor (injeção de dependência).
5. `@EnableScheduling` faz o método `executarColeta()` (anotado com `@Scheduled`) rodar logo ao iniciar e depois
   a cada `coleta.intervalo-ms` (10.000 ms), sempre 10 s **após o término** da coleta anterior (`fixedDelay`),
   sem execuções sobrepostas.
6. Ao encerrar (Ctrl+C/Stop), `@PreDestroy` fecha a `SessionFactory` do Hibernate.

**webapp — `WebappApplication`**

1. Mesma varredura de pacotes: encontra `ConsultaMeteorologicaService` (`@Service`).
2. `@Bean leituraMeteorologicaDAO()` entrega o DAO; `@Bean leituraMeteorologicaServlet(...)` cria o
   `LeituraMeteorologicaServlet` e o registra em `/api/leituras/*`. É o mesmo papel do mapeamento
   `<servlet>`/`<servlet-mapping>` de um `web.xml`.
3. O `spring-boot-starter-web` sobe o **Tomcat embutido** na porta 8080 (`server.port`). Não é preciso instalar Tomcat.
4. Os arquivos de `src/main/webapp` são copiados no build para `static/` (configuração `<resources>` no
   `webapp/pom.xml`) e entregues em `/`, `/css/style.css`, `/js/dashboard.js` e `/js/grafico.js`.

### 4.4 Anotações do Spring usadas

| Anotação | Onde | Significado |
|---|---|---|
| `@SpringBootApplication` | `CollectorApplication`, `WebappApplication` | Classe principal; liga configuração automática e varredura de componentes. |
| `@EnableScheduling` | `CollectorApplication` | Ativa o agendador de tarefas. |
| `@Scheduled` | `ColetaMeteorologicaService.executarColeta()` | Executa o método periodicamente (10 s). |
| `@Component` | `ApiMeteorologicaClient` | "Spring, crie e gerencie este objeto." |
| `@Service` | `ColetaMeteorologicaService`, `ConsultaMeteorologicaService` | Igual ao `@Component`, indicando a camada de serviço. |
| `@Bean` | métodos em `app/` | Cria manualmente um objeto que o Spring passa a gerenciar (DAO e Servlet). |
| `@Value("${...}")` | construtor do `ApiMeteorologicaClient`, `ColetaMeteorologicaService` | Injeta um valor do `application.properties`/`.env`. |
| `@PreDestroy` | `app/` | Método executado ao encerrar a aplicação (fecha o Hibernate). |

### 4.5 Configurações (`application.properties` + `.env`)

`spring.config.import=optional:file:.env[.properties],optional:file:../.env[.properties]` faz o Spring ler o `.env`
da raiz do projeto. As propriedades usam `${VARIAVEL:padrão}`:

| Propriedade | Variável no `.env` | Padrão |
|---|---|---|
| `openmeteo.latitude` / `openmeteo.longitude` | `OPENMETEO_LATITUDE` / `OPENMETEO_LONGITUDE` | São Paulo (-23.5505 / -46.6333) |
| `coleta.intervalo-ms` | `COLETA_INTERVALO_MS` | 10000 |
| `server.port` (webapp) | `SERVIDOR_PORTA` | 8080 |

Os dados do banco (`MYSQL_HOST`, `MYSQL_PORT`, `MYSQL_DATABASE`, `MYSQL_USER`, `MYSQL_PASSWORD`) são lidos pela
classe `Configuracao` e aplicados pelo `HibernateUtil`, não pelo Spring.

### 4.6 Por que usar Spring Boot aqui

- Já estava configurado no repositório do grupo desde o início.
- O enunciado diz que "a complexidade das tecnologias utilizadas" influencia a nota.
- Elimina código repetitivo (servidor web, agendador, leitura de configuração), deixando o foco nas partes avaliadas:
  coleta, DAO, Hibernate e página.
- Existe uma versão **sem Spring Boot** na branch `isaque2` (Servlet em Tomcat embutido + `ScheduledExecutorService`),
  caso o professor peça a solução sem framework. O módulo `compartilhado` (Hibernate + DAO) é praticamente igual nas duas.

---

## 5. Hibernate e DAO

### 5.1 Conexão — `HibernateUtil` + `hibernate.cfg.xml`

```java
Configuration configuration = new Configuration();
configuration.configure();                                  // lê hibernate.cfg.xml
configuration.setProperty("hibernate.connection.url", Configuracao.urlBanco());       // do .env
configuration.setProperty("hibernate.connection.username", Configuracao.usuarioBanco());
configuration.setProperty("hibernate.connection.password", Configuracao.senhaBanco());
sessionFactory = configuration.buildSessionFactory();
```

- O `hibernate.cfg.xml` tem o driver do MySQL, `hbm2ddl.auto=update`, o tamanho do pool e o mapeamento
  `<mapping class="br.com.aps.meteorologia.model.LeituraMeteorologica"/>`.
- **A senha não fica no XML**, porque o arquivo vai para o GitHub. URL, usuário e senha vêm do `.env`.
- A `SessionFactory` é criada uma vez, na primeira utilização. Se o MySQL estiver fora do ar, é lançada uma
  `PersistenciaException` e a próxima chamada tenta conectar de novo (o coletor não para).

### 5.2 Operações no DAO

```java
Session sessao = null;
Transaction transacao = null;
try {
    sessao = HibernateUtil.getSessionFactory().openSession();
    transacao = sessao.beginTransaction();
    sessao.persist(leitura);
    transacao.commit();
    return leitura;
} catch (RuntimeException e) {
    desfazer(transacao);                                    // rollback
    throw falha("Erro ao salvar a leitura", e);             // PersistenciaException
} finally {
    fechar(sessao);                                         // session.close()
}
```

Consultas usam HQL (`from LeituraMeteorologica l order by l.dataHoraColeta desc`) com a mesma estrutura
de abrir/fechar sessão.

| Método | Retorno |
|---|---|
| `salvar(LeituraMeteorologica)` | a leitura com `id` preenchido |
| `buscarUltima()` | `Optional` com a leitura mais recente |
| `buscarUltimas(int limite)` | as N mais recentes, **em ordem cronológica** (da mais antiga para a mais recente) |
| `buscarPorPeriodo(LocalDateTime inicio, LocalDateTime fim)` | leituras do intervalo (inclusive), em ordem cronológica |
| `contar()` | total de leituras |

---

## 6. Orientação a objetos aplicada

- **Encapsulamento:** atributos privados na entidade, acesso por getters/setters.
- **Abstração e polimorfismo:** serviços dependem da interface `LeituraMeteorologicaDAO`; a implementação
  Hibernate é entregue pelo Spring. Nos testes, o DAO é trocado por um simulado sem mudar o serviço.
- **Herança:** `LeituraMeteorologicaServlet extends HttpServlet`; exceções próprias estendem `RuntimeException`.
- **Responsabilidade única:** cada classe tem uma função (cliente HTTP, regra de coleta, acesso a dados, Servlet).
- **Exceções próprias:** `ApiMeteorologicaException` (falhas recuperáveis da API) e `PersistenciaException` (banco).
- **Records** (`RespostaOpenMeteo`, `ErroResposta`) para objetos imutáveis de transporte de dados.

## 7. Tratamento de erros

**collector:** nenhuma falha recuperável derruba o processo. O erro vai para o log e a próxima coleta segue normalmente.

| Situação | Tratamento |
|---|---|
| Timeout (10 s) / sem rede | `ApiMeteorologicaException` → log `WARN` |
| HTTP 429 (limite da API) | `ApiMeteorologicaException` com mensagem específica → log `WARN` |
| HTTP 4xx/5xx | `ApiMeteorologicaException` com o status → log `WARN` |
| JSON inválido / sem `current` / sem nenhuma variável | `ApiMeteorologicaException` → log `WARN` |
| MySQL fora do ar (na partida ou durante a execução) | `PersistenciaException` → log `ERROR`; reconecta na coleta seguinte |

Os logs não mostram credenciais.

**webapp:** sempre responde JSON no formato `{"status": 400, "erro": "mensagem", "dataHora": "..."}`.

| Situação | HTTP |
|---|---|
| Banco sem leituras (`/ultima`) | 404 `"Nenhuma leitura registrada ainda"` |
| Parâmetro inválido/ausente | 400 |
| MySQL indisponível | 503 `"Banco de dados indisponível no momento"` |
| Erro inesperado | 500 |

## 8. Contrato com o front-end (Servlet `/api/leituras`)

Base: `http://localhost:8080`. Datas em ISO 8601 (`2026-10-20T22:20:05`). Campos sem valor vêm como `null`.

| Endpoint | Descrição |
|---|---|
| `GET /api/leituras/ultima` | leitura mais recente (404 se vazio) |
| `GET /api/leituras?limite=100` | últimas N leituras (1–1000, padrão 100), **já em ordem cronológica** para o gráfico; `[]` se vazio |
| `GET /api/leituras/periodo?inicio=2026-10-20T00:00:00&fim=2026-10-20T23:59:59` | leituras do período (máx. 31 dias) |
| `GET /api/leituras/total` | `{"total": 123}` |

Exemplo de leitura:

```json
{
  "id": 12,
  "dataHoraColeta": "2026-10-20T22:39:01.983",
  "dataHoraFonte": "2026-10-20T22:30:00",
  "temperatura": 14.0,
  "umidadeRelativa": 90,
  "precipitacao": 0.0,
  "chuva": 0.0,
  "velocidadeVento": 9.9,
  "codigoClima": 3,
  "presencaPrecipitacao": false,
  "fonteDados": "Open-Meteo",
  "descricaoClima": "Nublado"
}
```

A página fica em `webapp/src/main/webapp/` (`index.html`, `css/style.css`, `js/dashboard.js`, `js/grafico.js`).
Os arquivos atuais são provisórios: `dashboard.js` já consulta o Servlet, e `grafico.js` documenta o formato
de entrada do gráfico e aguarda a implementação do front-end.
