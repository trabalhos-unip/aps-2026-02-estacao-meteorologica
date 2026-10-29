# Replanejamento — probabilidade de chuva

## Objetivo combinado

Disponibilizar a probabilidade de chuva por horário, obtida da Open-Meteo, persistida no banco e exposta para a interface. O requisito original previa apenas a coluna horária `probabilidade_chuva`.

## Escopo planejado

1. Solicitar `precipitation_probability` na série horária da Open-Meteo.
2. Persistir a probabilidade como inteiro de 0 a 100 em `resumo_horario_meteorologico`.
3. Retornar o valor no DTO e no endpoint de resumos horários.
4. Exibir a porcentagem no painel de clima por hora e no card de chuva.

## Ampliações incorporadas

Além do requisito inicial, o trabalho passou a incluir:

- probabilidade máxima diária em `resumo_diario_meteorologico`, para a previsão dos próximos dias;
- carga automática de histórico e previsão por cidade, sem depender de o computador ficar ligado por dias;
- 30 dias anteriores de histórico e 15 dias futuros de previsão;
- seleção de cidade com geocodificação, estação ativa isolada e atualização sem recarregar a página;
- painel de previsão diária e painel horário com ícones Meteocons;
- gráficos do histórico com o mesmo padrão visual dos gráficos principais;
- distinção entre histórico, previsão e leitura local. A leitura local tem prioridade para a mesma hora;
- migration única V2, consolidando a estrutura de estação, resumos por estação e probabilidades de chuva.

## Modelo de dados final

| Tabela | Campo | Regra |
| --- | --- | --- |
| `resumo_horario_meteorologico` | `probabilidade_chuva` | `INT`, de 0 a 100, nulo se a fonte não informar |
| `resumo_diario_meteorologico` | `probabilidade_chuva_maxima` | maior probabilidade horária do dia, de 0 a 100 |
| `estacao` | cidade, estado e país | identifica a localização ativa e preserva o histórico das demais |

As restrições, índices e relacionamentos estão em `database/schema.sql` e na migration consolidada `database/migrations/V2__estacao_log_e_variaveis_extras.sql`.

## Fluxo de dados

```text
Open-Meteo
  -> série horária com precipitation_probability
  -> collector
  -> resumos horários e diários por estação
  -> API REST
  -> dashboard, previsão e histórico
```

## Regras operacionais

- Dias encerrados são gravados como `HISTORICO`; o dia atual e os futuros como `PREVISAO`.
- Para o histórico do dia atual, a interface mostra apenas as horas que já ocorreram. Horas futuras não são preenchidas artificialmente.
- Ao trocar de cidade, a estação anterior permanece no banco; a aplicação consulta somente a estação ativa.
- A migration V2 consolidada deve ser usada somente em bancos anteriores à V2 ou em uma instalação limpa. Bancos que já receberam a sequência antiga V2–V6 não devem reaplicá-la.
