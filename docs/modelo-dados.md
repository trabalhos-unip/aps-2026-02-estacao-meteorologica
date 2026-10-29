# Modelo de dados

O banco MySQL do projeto se chama `estacao_meteorologica` e possui cinco tabelas: `estacao`, `leitura_meteorologica`, `log_coleta`, `resumo_diario_meteorologico` e `resumo_horario_meteorologico`.

- [schema.sql](../database/schema.sql) cria uma instalação nova.
- [V2__estacao_log_e_variaveis_extras.sql](../database/migrations/V2__estacao_log_e_variaveis_extras.sql) atualiza a estrutura antiga sem apagar leituras.
- [V3__resumos_por_estacao.sql](../database/migrations/V3__resumos_por_estacao.sql) vincula os resumos já existentes à estação correta.
- [V4__estacao_brasileira_ativa.sql](../database/migrations/V4__estacao_brasileira_ativa.sql) identifica cidade, UF e país, além de definir a estação ativa.
- [dados-teste.sql](../database/dados-teste.sql) insere leituras de exemplo depois de a estação existir.

## Relacionamentos

`estacao` 1 --- N `leitura_meteorologica`

`estacao` 1 --- N `log_coleta`

`leitura_meteorologica` 1 --- N `log_coleta` (somente para coletas bem-sucedidas)

`estacao` 1 --- N `resumo_diario_meteorologico`

`estacao` 1 --- N `resumo_horario_meteorologico`

O sistema começa com **Sorocaba/SP**, nas coordenadas `-23.5015, -47.4526`. Pela tela de configurações o usuário informa cidade e UF brasileiras; o webapp resolve a localização na Geocoding API da Open-Meteo e persiste a estação. Só uma estação permanece `ativa` por vez. Leituras e resumos não são compartilhados entre cidades, e o collector lê a estação ativa em cada ciclo.

## Tabela `estacao`

| Coluna | Tipo | Finalidade |
|---|---|---|
| `id` | `BIGINT` | Identificador da estação. |
| `nome` | `VARCHAR(100)` | Chave técnica única, no formato `BR:UF:CIDADE_NORMALIZADA`. |
| `cidade`, `estado`, `pais` | `VARCHAR` | Identificação da localização; `pais` é restrito a `BR`. |
| `latitude`, `longitude` | `DOUBLE` | Posição configurada para a coleta. |
| `fuso_horario` | `VARCHAR(50)` | Fuso usado pela estação; padrão `America/Sao_Paulo`. |
| `ativa` | `BIT(1)` | Indica a única estação usada pelas coletas e consultas atuais. |
| `criada_em` | `DATETIME(6)` | Momento do cadastro. |

## Tabela `leitura_meteorologica`

Além das leituras já existentes, cada linha possui `estacao_id`, que referencia a estação responsável pela coleta. `latitude` e `longitude` permanecem na leitura porque representam o ponto efetivamente retornado pela fonte de dados.

| Coluna | Origem | Unidade |
|---|---|---|
| `data_hora_coleta` | Relógio do coletor | data/hora de São Paulo |
| `data_hora_fonte` | `current.time` da Open-Meteo | data/hora de São Paulo |
| `temperatura` | `temperature_2m` | °C |
| `umidade_relativa` | `relative_humidity_2m` | % |
| `precipitacao`, `chuva` | `precipitation`, `rain` | mm |
| `velocidade_vento`, `direcao_vento`, `rajada_vento` | campos `wind_*` | km/h e graus |
| `codigo_clima` | `weather_code` | código WMO |
| `sensacao_termica` | `apparent_temperature` | °C |
| `pressao_superficie` | `surface_pressure` | hPa |
| `cobertura_nuvens` | `cloud_cover` | % |
| `modo_operacao` | Aplicação | `REAL` ou `SIMULADO` |
| `fonte_dados` | Aplicação | `Open-Meteo` ou `Simulador` |
| `presenca_precipitacao` | Regra da aplicação | verdadeiro quando há precipitação ou chuva maior que zero |
| `dado_repetido` | Regra da aplicação | verdadeiro quando `data_hora_fonte` se repete no ciclo seguinte |

Valores ausentes são gravados como `NULL`; não são substituídos por zero. A chave única `(estacao_id, data_hora_coleta)` preserva uma leitura por ciclo de coleta para cada estação.

## Tabela `log_coleta`

Registra o resultado de cada tentativa do coletor. Uma tentativa bem-sucedida informa `leitura_id`; uma falha informa `tipo_erro`, mensagem resumida e, quando aplicável, o código HTTP.

Os status são `SUCESSO` e `ERRO`. Os tipos de erro são `TIMEOUT`, `REDE`, `LIMITE_API`, `HTTP`, `RESPOSTA_INVALIDA`, `BANCO` e `OUTRO`. Se o próprio banco estiver indisponível, o coletor registra a falha no console e continua a executar os próximos ciclos.

## Resumos diário e horário

Os dois resumos recebem `estacao_id`, `tipo` (`HISTORICO` ou `PREVISAO`), a origem e o instante de atualização.
O diário consolida mínima, máxima, média, umidade, precipitação, vento e código WMO por data. O horário mantém a
série individual por hora. O coletor busca 30 dias anteriores e 15 dias futuros na Open-Meteo ao iniciar, a cada seis
horas e até 10 segundos após uma troca de estação; leituras locais continuam sendo o registro das condições atuais.

## Regras e validações

- Coordenadas, temperatura, umidade, precipitação, vento, pressão e nuvens possuem limites validados pelo MySQL.
- `modo_operacao`, `status` e `tipo_erro` têm valores restritos por `CHECK`.
- As chaves estrangeiras impedem apagar uma estação que tenha dados associados.
- O Hibernate usa `validate`: ele apenas confere se as entidades Java correspondem ao banco; não cria nem altera tabelas.
