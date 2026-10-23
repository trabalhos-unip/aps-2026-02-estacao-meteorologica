# Planejamento da APS — Estação Meteorológica (versão 2)
**Curso:** Ciência da Computação — 4º semestre  
**Disciplina:** Atividades Práticas Supervisionadas (APS) — 2026/2  
**Data do planejamento:** 14/10/2026  
**Prazo de entrega informado pelo grupo:** 12/11/2026  
**Equipe:** Roger, Isaque, Leonardo, João Otávio e João Guilherme

> **Base principal:** documento do professor “APS 3 e 4 — 2º semestre de 2026”, com as decisões já confirmadas pelo grupo incorporadas nesta versão: **repositório GitHub já criado, banco de dados MySQL e API Open-Meteo**. Os pontos que ainda não foram definidos estão indicados como decisões pendentes/recomendações.

---

## 1. Contexto e objetivo do projeto

O trabalho consiste em desenvolver um **sistema de estação meteorológica para monitoramento de variáveis ambientais**, usando uma API de previsão/condições meteorológicas para obter dados, armazená-los em banco de dados e disponibilizá-los em uma página Web.

O sistema deve ser composto por **duas aplicações distintas**:

1. **Coletor de dados:** conecta-se à API meteorológica, coleta os dados a cada **10 segundos para fins de teste** e os armazena no banco.
2. **Aplicação Web:** consulta os dados armazenados e os apresenta em uma página acessível por navegador, com HTML e CSS e um gráfico das variáveis ambientais ao longo do tempo.

### Variáveis previstas no enunciado

- Temperatura ambiente.
- Umidade relativa do ar.
- Presença ou ausência de precipitação.
- Velocidade do ar/vento.
- Outras variáveis disponíveis na API escolhida, se viável.

### Tecnologias exigidas ou permitidas no documento

- **Linguagem:** Java.
- **Persistência/ORM:** Hibernate.
- **Web:** tecnologias Web, incluindo HTML e CSS na apresentação.
- **Padrão de projeto:** DAO, além dos conceitos de orientação a objetos.
- **Banco definido pelo grupo:** MySQL.
- **Visualização:** navegador Web.

### Critério central de sucesso

O protótipo deve estar **100% funcional**: coletar dados reais da API, salvar no banco, consultar os registros e exibi-los corretamente na Web, incluindo o gráfico temporal. O professor também informa que tratamento de erros, refinamento e complexidade das tecnologias influenciam a nota.

### Escopo recomendado para não perder o prazo

Priorizar um fluxo completo e simples antes de adicionar extras:

1. Testar a chamada da Open-Meteo e confirmar os campos necessários para a localização escolhida.
2. Criar o banco MySQL e o modelo de dados.
3. Implementar a coleta periódica e a persistência via Hibernate.
4. Implementar o acesso aos dados usando DAO.
5. Criar a página Web com dados coletados e gráfico temporal.
6. Tratar falhas de rede/API e erros de persistência.
7. Testar o fluxo completo e registrar evidências para a monografia.

**Importante:** o documento do professor não determina um framework Web específico, a estrutura exata do DAO nem a quantidade de páginas. A API e o banco já foram definidos pelo grupo. A Open-Meteo fornece dados meteorológicos provenientes de modelos meteorológicos, não medições feitas por sensores físicos instalados pelo grupo. A monografia deve explicar essa diferença e não afirmar que foram feitas medições locais por sensores físicos se isso não ocorreu.


### Decisões técnicas já tomadas: Open-Meteo + MySQL + GitHub

**Repositório GitHub:** já existe. Antes de criar novas pastas, confiram os arquivos e a estrutura atual do repositório para não duplicar trabalho nem sobrescrever implementações existentes. Este planejamento propõe uma organização-alvo, que deve ser adaptada ao projeto real.

**Banco MySQL:** será o banco oficial do projeto. Sugestão de nome do schema: `aps_meteorologia`. Roger deve coordenar a definição da tabela, a criação do `schema.sql`, o usuário de desenvolvimento e a forma de conexão. As credenciais locais não devem ser enviadas ao GitHub.

**API Open-Meteo:** ponto de partida recomendado: [documentação oficial da Forecast API](https://open-meteo.com/en/docs). A API permite solicitar condições meteorológicas atuais e variáveis como temperatura a 2 m, umidade relativa, precipitação, chuva, velocidade do vento a 10 m e código meteorológico. Conferir a documentação para os nomes exatos dos parâmetros e as unidades utilizadas.

Exemplo de URL de teste (substituir latitude e longitude pelas coordenadas escolhidas para o projeto):

```text
https://api.open-meteo.com/v1/forecast?latitude=LATITUDE&longitude=LONGITUDE&current=temperature_2m,relative_humidity_2m,precipitation,rain,wind_speed_10m,weather_code&timezone=America%2FSao_Paulo
```

Parâmetros e campos sugeridos para o protótipo:

| Campo da resposta | Uso no projeto | Unidade padrão indicada na documentação |
|---|---|---|
| `current.time` | Horário associado às condições retornadas pela API | Data/hora conforme parâmetro `timezone` |
| `current.temperature_2m` | Temperatura ambiente aproximada a 2 metros | °C |
| `current.relative_humidity_2m` | Umidade relativa do ar | % |
| `current.precipitation` | Precipitação | mm |
| `current.rain` | Chuva | mm |
| `current.wind_speed_10m` | Velocidade do vento a 10 metros | km/h |
| `current.weather_code` | Código da condição meteorológica | Código WMO; consultar tabela na documentação |

**Ponto importante sobre os 10 segundos:** o enunciado exige que o programa faça a coleta a cada 10 segundos para fins de teste. Porém, a Open-Meteo informa que as condições atuais são baseadas em dados de modelos meteorológicos com resolução de 15 minutos. Assim, várias consultas feitas dentro desse intervalo podem retornar o mesmo horário de origem e valores repetidos. A cada ciclo, o sistema pode registrar o horário da consulta (`dataHoraColeta`) e guardar separadamente o `current.time` da API (`dataHoraFonte`). Na monografia, expliquem essa característica da fonte e não apresentem as consultas como novas medições feitas por um sensor físico a cada 10 segundos. Fonte: [Open-Meteo — Forecast API](https://open-meteo.com/en/docs).

**Limites de uso:** a página oficial de termos informa, para a API gratuita de uso não comercial, limites de chamadas diárias, por hora e por minuto. Como uma chamada a cada 10 segundos durante 24 horas equivale a 8.640 chamadas por dia, façam a coleta em períodos controlados de teste e confiram os [termos vigentes da Open-Meteo](https://open-meteo.com/en/terms). A coleta de uma localização e de várias variáveis numa mesma requisição tende a ser mais simples do que realizar uma chamada por variável.

---

## 2. Checklist geral de entregas

### 2.1. Planejamento e decisões iniciais

- [ ] Confirmar com o professor eventuais dúvidas sobre o escopo, formato da entrega e demonstração.
- [ ] Definir quem será o **líder do grupo** (o enunciado exige a designação).
- [x] Repositório GitHub criado pelo grupo.
- [ ] Conferir a estrutura atual do repositório antes de criar ou mover pastas; preservar o que já foi desenvolvido.
- [ ] Combinar regras de branches, commits, revisão e resolução de conflitos.
- [x] API definida: **Open-Meteo**.
- [x] Banco definido: **MySQL**.
- [ ] Testar a chamada à Open-Meteo e documentar os campos, unidades, localização e limites relevantes.
- [ ] Definir como as duas aplicações serão executadas e como compartilharão o banco MySQL.
- [ ] Definir o mecanismo Web Java a utilizar, mantendo o escopo simples.
- [ ] Criar/atualizar o quadro de tarefas e marcar responsáveis e prazos.

### 2.2. Banco de dados e persistência (MySQL)

- [ ] Criar o banco MySQL do projeto (nome sugerido: `aps_meteorologia`).
- [ ] Definir as variáveis e os campos que serão armazenados.
- [ ] Criar o modelo de dados e documentar as decisões.
- [ ] Adicionar/configurar o driver JDBC do MySQL e a configuração do Hibernate.
- [ ] Implementar entidades Java mapeadas para o banco.
- [ ] Implementar DAO para operações de persistência e consulta.
- [ ] Validar inserção, consulta e ordenação temporal dos registros.
- [ ] Definir como serão armazenados data/hora, unidades e valores ausentes.
- [ ] Evitar duplicação acidental de registros e tratar falhas de conexão.

### 2.3. Coleta da API Open-Meteo

- [ ] Fazer uma chamada manual à API Forecast da Open-Meteo e entender o formato da resposta JSON.
- [ ] Implementar o cliente HTTP e a leitura da resposta.
- [ ] Converter os dados da API para um modelo interno consistente.
- [ ] Programar a coleta a cada 10 segundos durante os testes.
- [ ] Salvar cada coleta no banco usando a camada DAO.
- [ ] Tratar timeout, indisponibilidade da API, respostas inválidas e limites de requisição.
- [ ] Registrar logs úteis sem expor credenciais do banco ou outras configurações sensíveis.
- [ ] Registrar separadamente o horário em que a consulta foi feita e, quando disponível, o horário (`time`) associado aos dados retornados pela API.
- [ ] Confirmar que o processo continua funcionando após uma falha recuperável.

### 2.4. Aplicação Web e visualização

- [ ] Criar uma página HTML/CSS organizada e legível.
- [ ] Consultar os dados por meio da camada de acesso definida, sem duplicar regras de persistência.
- [ ] Exibir os dados mais recentes.
- [ ] Exibir histórico dos dados.
- [ ] Plotar gráfico temporal com as variáveis ambientais disponíveis.
- [ ] Identificar claramente eixos, unidades, data e hora.
- [ ] Exibir mensagem apropriada quando não houver dados ou ocorrer um erro.
- [ ] Verificar o funcionamento em um navegador comum.

### 2.5. Testes e demonstração

- [ ] Testar conexão com a API.
- [ ] Testar coleta e persistência repetidas em intervalos de 10 segundos.
- [ ] Testar consulta ao banco e ordenação por data/hora.
- [ ] Testar atualização da página e gráfico.
- [ ] Simular indisponibilidade da API e resposta inválida, quando possível.
- [ ] Testar banco vazio e valores ausentes.
- [ ] Demonstrar o fluxo completo: API → coletor → banco → aplicação Web → gráfico.
- [ ] Salvar capturas de tela, resultados e evidências dos testes para a monografia.
- [ ] Fazer uma execução final em ambiente limpo, seguindo as instruções do README.

### 2.6. Monografia/documentação acadêmica

O professor exige que a monografia siga o **Guia de Normalização para Apresentação de Trabalhos Acadêmicos da UNIP (ABNT)** e inclua:

- [ ] Breve descrição das tecnologias utilizadas.
- [ ] Explicação da linguagem de programação, publicação dos dados e algoritmos de coleta.
- [ ] Pesquisa sobre estações meteorológicas físicas e sensores utilizados.
- [ ] Descrição das etapas do desenvolvimento: obtenção, publicação e apresentação dos dados no navegador.
- [ ] Influência da meteorologia no modelo socioeconômico.
- [ ] Calibração, aferição e precisão de sensores.
- [ ] Órgãos nacionais responsáveis pela meteorologia.
- [ ] Órgãos nacionais de aferição e calibração de instrumentos e sensores.
- [ ] **No mínimo dois artigos acadêmicos**, corretamente citados.
- [ ] Conjunto de testes que demonstre o funcionamento e simule condições meteorológicas.
- [ ] Considerações do grupo sobre as técnicas escolhidas e os resultados obtidos.
- [ ] Referências bibliográficas de todas as fontes utilizadas.
- [ ] Código-fonte completo como apêndice (o enunciado informa que o código não precisa seguir a formatação ABNT).
- [ ] Apresentação de um protótipo funcional.
- [ ] Revisão final de citações, referências, formatação e ortografia.
- [ ] Fichas individuais dos integrantes preenchidas cronologicamente conforme orientação do professor.
- [ ] Líder responsável por postar o trabalho no sistema indicado pela UNIP, respeitando o calendário acadêmico.

---

## 3. Cronograma de 14/10 a 12/11

O prazo é curto: são aproximadamente quatro semanas. A meta é ter uma versão funcional mínima até o fim de outubro, deixando a primeira semana de novembro para integração, testes e monografia.

| Período | Objetivo | Atividades principais | Entrega verificável |
|---|---|---|---|
| **14–15/10** | Alinhamento e preparação do repositório existente | Ler o enunciado em grupo; designar líder; conferir o repositório GitHub já criado; confirmar Open-Meteo e MySQL; decidir abordagem Web; dividir tarefas; atualizar README e quadro de atividades. | Decisões confirmadas, tarefas atribuídas e repositório existente organizado sem apagar conteúdo. |
| **16–18/10** | Projeto técnico | Testar chamada à Open-Meteo; definir coordenadas/localização e variáveis; mapear campos e unidades; definir tabela MySQL e entidade; desenhar estrutura DAO; preparar esqueleto das duas aplicações conforme a estrutura atual do repositório. | Chamada à Open-Meteo testada, modelo de dados definido e estrutura de pastas acordada. |
| **19–22/10** | MySQL e coleta inicial | Criar schema/tabela no MySQL; configurar driver JDBC e Hibernate; implementar entidade e DAO; implementar primeira chamada Java à Open-Meteo e salvar ao menos um registro. | Registro vindo da Open-Meteo persistido e recuperável no MySQL. |
| **23–26/10** | Coleta automática e consultas | Implementar ciclo de 10 segundos; tratamento básico de erros; consultas por DAO; iniciar a aplicação Web e exibir os registros. | Coletor rodando e página exibindo dados do banco. |
| **27–30/10** | Gráfico e integração | Criar gráfico temporal; melhorar layout HTML/CSS; conectar as duas aplicações ao mesmo banco; corrigir incompatibilidades. | **Versão funcional mínima ponta a ponta**. |
| **31/10–02/11** | Testes técnicos | Testar coleta contínua, banco vazio, falhas da API, dados ausentes, ordenação e gráfico; corrigir defeitos prioritários. | Roteiro de testes preenchido e defeitos críticos resolvidos. |
| **03–05/11** | Monografia completa em primeira versão | Consolidar capítulos, pesquisa de sensores e meteorologia, artigos acadêmicos, referências, descrição do sistema e testes. | Monografia completa em versão de revisão. |
| **06–08/11** | Revisão cruzada | Integrantes revisam o trabalho uns dos outros; conferir citações/ABNT; revisar código, README e instruções de execução; repetir demonstração completa. | Candidata à versão final do protótipo e monografia revisada. |
| **09–10/11** | Congelamento e validação | Evitar funcionalidades novas; corrigir somente erros; testar em execução limpa; conferir todos os itens do enunciado e o apêndice com código. | Checklist final concluído e pacote de entrega preparado. |
| **11/11** | Margem de segurança | Revisão final, confirmar arquivos, fichas individuais, líder e forma de envio; fazer backup do projeto. | Tudo pronto antes do último dia. |
| **12/11** | Entrega | Líder realiza a postagem conforme calendário e instruções da UNIP; conferir confirmação de envio. | Trabalho enviado e comprovante guardado. |

**Regra de organização:** cada tarefa precisa ter um responsável principal e uma pessoa revisora. Façam uma reunião curta de 10–15 minutos a cada dois dias para identificar bloqueios. Se uma tarefa atrasar, priorizem o fluxo completo exigido pelo professor em vez de acrescentar funcionalidades extras.

---

## 4. Estrutura sugerida de pastas e arquivos — Java + Hibernate + DAO

A estrutura abaixo é uma **proposta de organização**, não uma estrutura imposta literalmente pelo documento do professor. Ela separa o coletor da aplicação Web e mantém responsabilidades claras. O nome do pacote Java deve ser ajustado ao padrão escolhido pelo grupo.

```text
aps-estacao-meteorologica/
├── README.md                         # Atualizar o README existente, se já houver
├── .gitignore                         # Garantir que credenciais/arquivos locais fiquem ignorados
├── docs/
│   ├── planejamento.md
│   ├── modelo-dados.md
│   ├── arquitetura.md
│   ├── roteiro-testes.md
│   ├── evidencias/
│   └── monografia/
│       ├── monografia.docx
│       └── referencias.md
├── database/
│   ├── schema.sql                    # DDL compatível com MySQL
│   └── dados-teste.sql
├── collector/                         # Aplicação 1: API → banco
│   ├── pom.xml                        # Se o grupo escolher Maven
│   └── src/
│       ├── main/
│       │   ├── java/br/com/aps/meteorologia/
│       │   │   ├── app/
│       │   │   │   └── CollectorApplication.java
│       │   │   ├── config/
│       │   │   │   └── HibernateUtil.java
│       │   │   ├── model/
│       │   │   │   └── LeituraMeteorologica.java
│       │   │   ├── dao/
│       │   │   │   ├── LeituraMeteorologicaDAO.java
│       │   │   │   └── impl/
│       │   │   │       └── LeituraMeteorologicaDAOImpl.java
│       │   │   ├── service/
│       │   │   │   └── ColetaMeteorologicaService.java
│       │   │   ├── client/
│       │   │   │   └── ApiMeteorologicaClient.java
│       │   │   └── util/
│       │   │       └── DataHoraUtil.java
│       │   └── resources/
│       │       └── hibernate.cfg.xml
│       └── test/
├── webapp/                             # Aplicação 2: banco → navegador
│   ├── pom.xml                         # Se o grupo escolher Maven
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/br/com/aps/meteorologia/
│   │   │   │   ├── config/
│   │   │   │   │   └── HibernateUtil.java
│   │   │   │   ├── model/
│   │   │   │   │   └── LeituraMeteorologica.java
│   │   │   │   ├── dao/
│   │   │   │   │   ├── LeituraMeteorologicaDAO.java
│   │   │   │   │   └── impl/
│   │   │   │   │       └── LeituraMeteorologicaDAOImpl.java
│   │   │   │   ├── service/
│   │   │   │   │   └── ConsultaMeteorologicaService.java
│   │   │   │   └── controller/
│   │   │   │       └── LeituraMeteorologicaServlet.java
│   │   │   ├── resources/
│   │   │   │   └── hibernate.cfg.xml
│   │   │   └── webapp/
│   │   │       ├── index.html
│   │   │       ├── css/
│   │   │       │   └── style.css
│   │   │       ├── js/
│   │   │       │   ├── dashboard.js
│   │   │       │   └── grafico.js
│   │   │       └── WEB-INF/
│   │   │           └── web.xml         # Se necessário à tecnologia escolhida
│   │   └── test/
└── scripts/
    ├── iniciar-coletor.md
    └── executar-web.md
```

### Responsabilidade de cada camada

| Camada/pasta | Responsabilidade |
|---|---|
| `model/` | Representar uma leitura meteorológica como objeto Java e mapear seus campos para o banco via Hibernate. |
| `dao/` | Declarar operações de acesso aos dados, por exemplo salvar uma leitura, buscar as últimas leituras e consultar um intervalo de datas. |
| `dao/impl/` | Implementar as operações DAO usando Hibernate. A regra de acesso ao banco fica concentrada aqui. |
| `client/` | Fazer a comunicação HTTP com a API meteorológica e obter a resposta. |
| `service/` | Coordenar as regras do caso de uso: obter dados, validar/converter, salvar ou consultar. |
| `controller/` | Receber a requisição Web, chamar o serviço e devolver os dados à página. |
| `config/` | Centralizar a configuração e a criação/gerência de sessões do Hibernate. |
| `resources/` | Guardar configurações necessárias à aplicação; não versionar segredos. |
| `webapp/css/` e `webapp/js/` | Separar apresentação visual e lógica do gráfico da estrutura HTML. |
| `database/` | Manter scripts SQL para criação e, se necessário, dados de teste. |
| `docs/` | Concentrar arquitetura, roteiro de testes, evidências e documentação acadêmica. |

### Fluxo esperado

```text
APLICAÇÃO 1 — COLETOR
API meteorológica
       ↓
ApiMeteorologicaClient
       ↓
ColetaMeteorologicaService
       ↓
LeituraMeteorologica (model)
       ↓
LeituraMeteorologicaDAO
       ↓
Hibernate
       ↓
Banco de dados

APLICAÇÃO 2 — WEB
Navegador → Controller/Servlet → Service → DAO → Hibernate → Banco
     ↑                                                   ↓
     └──────────── HTML/CSS + gráfico ← dados consultados ┘
```

### Exemplo de campos para a entidade `LeituraMeteorologica`

Ajustar os campos de acordo com a resposta da Open-Meteo. Não armazenar campos que a API não fornece.

- `id`: identificador único.
- `dataHoraColeta`: instante em que o sistema consultou/salvou a resposta.
- `dataHoraFonte`: horário `time` associado ao conjunto de condições retornadas pela API, caso seja útil manter essa informação separada.
- `temperatura`: corresponde a `temperature_2m`, em °C por padrão.
- `umidadeRelativa`: corresponde a `relative_humidity_2m`, em porcentagem.
- `precipitacao`: corresponde a `precipitation`, em milímetros.
- `chuva`: campo `rain`, em milímetros, opcional para detalhar chuva.
- `velocidadeVento`: corresponde a `wind_speed_10m`, em km/h por padrão.
- `codigoClima`: corresponde a `weather_code`, para descrever a condição meteorológica.
- `presencaPrecipitacao`: indicador derivado conforme uma regra que o grupo deve documentar (por exemplo, precipitação maior que zero).
- `fonteDados`: valor fixo identificando Open-Meteo, se desejado.

O grupo deve diferenciar o **horário de consulta/gravação** do horário (`time`) associado aos dados meteorológicos retornados pela Open-Meteo. Como as condições atuais da API são baseadas em dados de modelos meteorológicos com resolução de 15 minutos, consultar a cada 10 segundos pode gerar registros consecutivos com os mesmos valores de origem. Isso não significa que a API forneça uma nova medição física a cada 10 segundos. O intervalo de 10 segundos é o ritmo de execução exigido para o teste do projeto.

### Cuidados importantes com DAO e Hibernate

- Não colocar SQL e operações Hibernate diretamente na página HTML ou no controller.
- Manter a interface DAO separada da implementação quando isso fizer sentido para o projeto.
- Encerrar sessões e transações corretamente e tratar exceções.
- Compartilhar entre as aplicações a mesma configuração de banco, sem duplicar regras de negócio.
- Não versionar a senha do MySQL nem arquivos locais que contenham credenciais. O acesso básico à Forecast API da Open-Meteo não exige uma chave de API; documentar apenas as configurações necessárias ao projeto.
- O intervalo de 10 segundos é para teste, conforme o enunciado. A Open-Meteo informa atualização/resolução das condições atuais em 15 minutos; portanto, evitem afirmar que os dados meteorológicos são atualizados a cada 10 segundos. Consultem os termos atuais da API e limitem as execuções de teste ao necessário.
- A estrutura pode mudar caso o professor exija um framework específico. Evitar implementar arquitetura complexa além do necessário.

---

## 5. Divisão de tarefas entre os cinco integrantes

A divisão respeita as preferências já informadas: **Roger fica com o banco de dados**, **Isaque com o back-end** e **Leonardo não fica responsável pela monografia/documentação acadêmica**. As demais alocações são propostas iniciais e podem ser ajustadas pelo grupo.

| Integrante | Responsabilidade principal | Tarefas concretas | Apoio/revisão |
|---|---|---|---|
| **Roger** | **Banco de dados e persistência** | Configurar o MySQL e modelar a tabela de leituras; criar `schema.sql`; apoiar o mapeamento Hibernate; validar inserções e consultas; documentar modelo de dados e instruções de configuração. | Revisar integração entre DAO e banco com Isaque; fornecer à monografia a explicação do modelo de dados. |
| **Isaque** | **Back-end Java e integração com API** | Implementar cliente da API; conversão/validação dos dados; serviço de coleta a cada 10 segundos; integração com DAO/Hibernate; tratamento de erros e logs; apoiar o serviço de consulta Web. | Fazer revisão técnica do fluxo com Roger; documentar decisões técnicas e algoritmos para a monografia. |
| **Leonardo** | **Front-end Web e experiência de visualização** | Criar HTML/CSS; organizar painel de dados; implementar o gráfico temporal com JavaScript/biblioteca compatível; mostrar unidades e datas; tratar estados sem dados/erro na interface; testar no navegador. | Integrar a página com o endpoint/controller definido com Isaque; entregar capturas de tela e descrição da interface, sem assumir a redação da monografia. |
| **João Otávio** | **Monografia: pesquisa teórica e referências** | Pesquisar estações meteorológicas e sensores; estudar influência da meteorologia no modelo socioeconômico; identificar órgãos nacionais de meteorologia e de aferição/calibração; encontrar pelo menos dois artigos acadêmicos; redigir e organizar referências corretamente. | Revisar fontes e citações com João Guilherme; pedir ao grupo validação técnica do conteúdo. |
| **João Guilherme** | **Monografia: desenvolvimento, testes e consolidação** | Redigir tecnologias, linguagem Java, coleta/publicação dos dados e etapas do sistema; organizar roteiro e resultados dos testes; escrever considerações finais; consolidar capítulos, apêndice do código e formatação ABNT. | Recolher de Roger, Isaque e Leonardo as explicações técnicas; fazer revisão cruzada com João Otávio. |

### Responsabilidades compartilhadas

- **Todos:** ler o enunciado, cumprir prazos, revisar o protótipo e participar da demonstração final.
- **Todos os responsáveis pelo código:** escrever comentários úteis, realizar commits pequenos e fornecer instruções de execução.
- **João Otávio e João Guilherme:** dividir a redação, mas cada integrante deve revisar a monografia inteira.
- **Roger e Isaque:** alinhar cedo o modelo de dados, a entidade Hibernate e os métodos DAO para evitar retrabalho.
- **Isaque e Leonardo:** combinar desde o início o formato de dados que o back-end entregará ao front-end.
- **Líder do grupo (a definir):** acompanhar o cronograma, convocar reuniões, conferir as fichas individuais e realizar a postagem final. O líder não precisa ser a única pessoa responsável pela entrega técnica.

---

## 6. Contratos de integração para reduzir retrabalho

Antes de programar em paralelo, o grupo deve concordar com estes pontos:

1. **API:** a fonte está definida como Open-Meteo; confirmar coordenadas/localização, variáveis, unidades, timezone e formato JSON.
2. **Banco:** o SGBD está definido como MySQL; confirmar nome do schema, usuário local e como cada integrante configurará a conexão.
3. **Entidade:** nomes dos campos, tipos Java, unidades e significado de cada valor.
4. **DAO:** métodos mínimos sugeridos:
   - `salvar(LeituraMeteorologica leitura)`
   - `buscarUltimas(int limite)`
   - `buscarPorPeriodo(dataInicio, dataFim)`
5. **Integração Web:** definir se a página receberá dados via Servlet/endpoint ou renderização do servidor, de acordo com a tecnologia escolhida.
6. **Gráfico:** definir formato dos dados de entrada e qual biblioteca será usada, se necessária.
7. **Configuração:** documentar a URL do banco MySQL e as configurações locais. A API Forecast de uso aberto da Open-Meteo normalmente pode ser consultada sem chave de API; não criar nem documentar uma chave inexistente. Nunca versionar senha do MySQL no repositório.

Esses nomes são sugestões de contrato, não exigências literais do professor.

---

## 7. Estratégia mínima de testes

| Teste | Procedimento | Resultado esperado |
|---|---|---|
| Open-Meteo disponível | Fazer uma chamada à Forecast API com coordenadas definidas e variáveis necessárias. | Resposta JSON válida e campos/unidades identificados. |
| Persistência | Coletar uma leitura e consultar o banco. | Registro salvo e recuperável. |
| Periodicidade | Executar o coletor durante alguns minutos. | Novas coletas aproximadamente a cada 10 segundos, respeitando a resposta e os limites da API. |
| Consulta histórica | Buscar várias leituras por data/hora. | Registros retornados em ordem temporal definida. |
| Gráfico | Abrir a página com registros existentes. | Gráfico apresenta as variáveis disponíveis ao longo do tempo, com eixos/unidades legíveis. |
| Banco vazio | Abrir a página antes de haver leituras. | Mensagem informativa; a página não quebra. |
| Falha da API | Simular desconexão ou resposta inválida. | Erro tratado e registrado; o sistema não encerra inesperadamente por uma falha recuperável. |
| Integração completa | Iniciar banco, coletor e Web e abrir a página. | Dados coletados são armazenados e aparecem na interface/gráfico. |
| Execução limpa | Seguir apenas o README em ambiente configurado do zero. | Outro integrante consegue iniciar o protótipo sem instruções orais adicionais. |

Guardar evidências dos testes e explicar na monografia quais condições foram simuladas. Não afirmar que um teste foi executado antes de realizá-lo.

---

## 8. Riscos e prioridades

| Risco | Como reduzir |
|---|---|
| Começar a monografia tarde | João Otávio e João Guilherme pesquisam e escrevem em paralelo ao desenvolvimento. |
| A integração à Open-Meteo ou uma variável esperada não funcionar como previsto | Testar a resposta da API até 18/10, revisar os nomes dos campos e registrar honestamente limitações. |
| Integração entre aplicações atrasar | Definir cedo o modelo de dados e o contrato do DAO/endpoint. |
| Coleta a cada 10 segundos atingir limite da API ou gerar dados repetidos | Usar o intervalo de 10 segundos apenas em períodos controlados de teste; considerar que a resolução meteorológica da API é maior que 10 segundos e respeitar os termos de uso. |
| Erros de configuração em máquinas diferentes | Documentar dependências, variáveis de ambiente e passos no README. |
| Alterações tardias quebrarem o protótipo | Congelar funcionalidades em 09/11 e priorizar correções críticas. |
| Monografia sem fontes acadêmicas suficientes | Buscar artigos desde a primeira semana e registrar imediatamente os dados bibliográficos. |

---

## 9. Critério de pronto para a entrega

O projeto só deve ser considerado pronto quando:

- [ ] As duas aplicações estão implementadas e consultam o mesmo banco MySQL.
- [ ] O coletor consulta a Open-Meteo e grava os registros em intervalos de 10 segundos durante os testes, distinguindo horário da consulta e horário associado aos dados da API.
- [ ] O acesso aos dados segue o padrão DAO e usa Hibernate.
- [ ] A página Web é acessível por navegador e mostra os dados armazenados.
- [ ] Há gráfico temporal das variáveis ambientais disponíveis.
- [ ] Erros básicos e ausência de dados são tratados.
- [ ] Os testes foram executados e documentados.
- [ ] A monografia contempla todos os tópicos exigidos, incluindo dois artigos acadêmicos no mínimo e referências.
- [ ] O código-fonte está incluído como apêndice.
- [ ] O README explica como configurar e executar o protótipo.
- [ ] A formatação acadêmica e as fichas individuais foram conferidas.
- [ ] O líder confirmou a postagem e guardou o comprovante.

## 10. Próximas ações — fazer primeiro

**Até o fim de 15/10:**

1. O grupo confirma a divisão de tarefas e escolhe o líder.
2. Roger cria/confere o schema MySQL e compartilha uma proposta inicial da tabela de leituras com Isaque.
3. Isaque faz a primeira chamada Java à Open-Meteo e compartilha os campos da resposta com Roger e Leonardo.
4. Leonardo define uma primeira proposta de tela e gráfico.
5. João Otávio começa a pesquisa acadêmica e registra fontes.
6. João Guilherme cria/atualiza o esqueleto da monografia e o checklist de requisitos.
7. Todos conferem o repositório GitHub já existente e combinam como registrar tarefas e alterações.

**Prioridade máxima:** chegar a **30/10 com o caminho API → banco → Web → gráfico funcionando**. A monografia deve evoluir em paralelo, e os últimos dias devem ser usados para revisão e correção, não para começar módulos essenciais.
