# Evidência - integração com MySQL local

**Data:** 24/10/2026
**Ambiente:** serviço local MySQL80, schema `estacao_meteorologica`.

## Estrutura criada e validada

- `database/schema.sql` foi aplicado sem erros.
- A tabela `leitura_meteorologica` foi criada com as 14 colunas previstas, incluindo `latitude`, `longitude`,
  `modo_operacao` e `fonte_dados`.
- Foram confirmados os índices `PRIMARY`, `idx_leitura_data_hora_coleta` e
  `idx_leitura_modo_data_hora` (`modo_operacao`, `data_hora_coleta`).
- O Hibernate foi alterado de `hbm2ddl.auto=update` para `validate`; o webapp iniciou e consultou o banco com
  essa validação ativa.

## Dados e integração

1. `database/dados-teste.sql` foi carregado em ambiente de desenvolvimento: 6 leituras `REAL` e 4 `SIMULADO`.
2. O coletor foi iniciado temporariamente em modo `SIMULADO` e gravou leituras pelo fluxo real da aplicação
   (fonte -> serviço -> DAO -> Hibernate -> MySQL).
3. O webapp respondeu `HTTP 200` para `GET /api/leituras/total` e retornou `66` leituras.
4. Ao fim da validação havia 6 leituras `REAL` e 60 `SIMULADO`. Das simuladas, 4 vêm do script de teste e 56
   foram geradas durante a execução controlada do coletor.
5. A última leitura consultada continha `modoOperacao=SIMULADO`, `fonteDados=Simulador`, latitude `-23.5505` e
   longitude `-46.6333`.
6. Após encerrar os processos temporários do coletor e do webapp, a contagem permaneceu estável em 66 por 15 s.

## Testes automatizados

Executados com Maven local do IntelliJ: **51 testes, 0 falhas, 0 erros e 0 ignorados**.

Nenhuma senha, conteúdo de `.env` ou dump com credenciais foi registrado neste documento.
