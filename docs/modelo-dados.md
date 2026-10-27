# Modelo de dados

O banco MySQL do projeto se chama `estacao_meteorologica` e possui três tabelas: `estacao`, `leitura_meteorologica` e `log_coleta`.

- [schema.sql](../database/schema.sql) cria uma instalação nova.
- [V2__estacao_log_e_variaveis_extras.sql](../database/migrations/V2__estacao_log_e_variaveis_extras.sql) atualiza a estrutura antiga sem apagar leituras.
- [dados-teste.sql](../database/dados-teste.sql) insere leituras de exemplo depois de a estação existir.

## Relacionamentos

`estacao` 1 --- N `leitura_meteorologica`

`estacao` 1 --- N `log_coleta`

`leitura_meteorologica` 1 --- N `log_coleta` (somente para coletas bem-sucedidas)

O sistema começa com uma única estação: **Estação Sorocaba**, nas coordenadas `-23.5015, -47.4526`. A tabela permite cadastrar novas estações futuramente, mas a interface de seleção ainda não faz parte do projeto.

## Tabela `estacao`

| Coluna | Tipo | Finalidade |
|---|---|---|
| `id` | `BIGINT` | Identificador da estação. |
| `nome` | `VARCHAR(100)` | Nome único da estação. |
| `latitude`, `longitude` | `DOUBLE` | Posição configurada para a coleta. |
| `fuso_horario` | `VARCHAR(50)` | Fuso usado pela estação; padrão `America/Sao_Paulo`. |
| `ativa` | `BIT(1)` | Indica se a estação pode ser usada. |
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

## Regras e validações

- Coordenadas, temperatura, umidade, precipitação, vento, pressão e nuvens possuem limites validados pelo MySQL.
- `modo_operacao`, `status` e `tipo_erro` têm valores restritos por `CHECK`.
- As chaves estrangeiras impedem apagar uma estação que tenha dados associados.
- O Hibernate usa `validate`: ele apenas confere se as entidades Java correspondem ao banco; não cria nem altera tabelas.
