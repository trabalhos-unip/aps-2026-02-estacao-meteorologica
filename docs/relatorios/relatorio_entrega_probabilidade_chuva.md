# Relatório de entrega — probabilidade de chuva

## Entrega realizada

O requisito de probabilidade de chuva por horário foi entregue e ampliado para sustentar previsão diária, histórico por cidade e visualização contínua na aplicação.

## Banco de dados

- Incluídos `probabilidade_chuva` no resumo horário e `probabilidade_chuva_maxima` no resumo diário.
- Mantidas validações de 0 a 100 por `CHECK`.
- Consolidada a evolução de banco em uma única migration: `database/migrations/V2__estacao_log_e_variaveis_extras.sql`.
- Removidas as antigas migrations V3, V4, V5 e V6, pois seu conteúdo foi incorporado à V2.
- Alinhado `database/schema.sql` com cidade, UF, país, resumos por estação e probabilidades de chuva.

## Back-end e coleta

- `ClienteOpenMeteo` solicita `precipitation_probability` na série horária.
- A resposta, entidade, persistência e DTO de resumo horário transportam `probabilidadeChuva`.
- A sincronização calcula a maior probabilidade horária para o resumo diário.
- O coletor carrega 30 dias anteriores e 15 dias futuros para a estação ativa.
- A separação entre `HISTORICO` e `PREVISAO` evita apresentar uma previsão como observação da estação.

## Interface

- Card **Chuva** exibe chance de chuva e precipitação em milímetros separadamente.
- Previsão diária apresenta ícone, chance de chuva e temperaturas máxima e mínima.
- Previsão por hora usa ícones Meteocons e apresenta horário, temperatura e chance de chuva.
- Histórico possui períodos Hoje, 7 dias e 30 dias, com gráficos consistentes e dados isolados por cidade.
- A troca de cidade atualiza os dados sem refresh visual da página.

## Validação

- Sintaxe dos módulos JavaScript verificada.
- Integridade do diff e presença dos campos consolidados na migration V2 verificadas.
- A suíte Maven precisa ser executada pelo NetBeans nesta máquina, pois o Maven não está disponível no `PATH` do terminal usado pela automação.

## Ação necessária para dados reais

Após compilar e reiniciar o `collector`, a próxima sincronização preencherá os 30 dias históricos e 15 dias previstos da estação ativa. O webapp deve ser recompilado/reiniciado para carregar os scripts atualizados.
