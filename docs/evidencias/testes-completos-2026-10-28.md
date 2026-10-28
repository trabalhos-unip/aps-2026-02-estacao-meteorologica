# Evidência - testes completos do sistema

**Data:** 28/10/2026
**Ambiente:** MySQL local, coletor em modo `REAL` e webapp (Spring Boot) em `http://localhost:8080`, todos rodando
juntos. Estação ativa: Itapetininga/SP. Os testes de interface usaram o Microsoft Edge (Playwright) com perfil
temporário. Nenhuma cidade foi trocada e nenhum dado foi gravado ou apagado pelos testes.

## Requisitos do enunciado

| Requisito (documento do professor) | Como foi verificado | Resultado |
|---|---|---|
| Uso de API de previsão do tempo | Valores da última leitura comparados com a Open-Meteo nas mesmas coordenadas | ✅ idênticos: 22,7 °C, 62 %, 7,4 km/h |
| Temperatura, umidade, presença de precipitação, velocidade do vento | Campos da última leitura em `GET /api/leituras/ultima` e na tela | ✅ todos preenchidos |
| Outras variáveis disponíveis | Campos extras da leitura | ✅ precipitação, chuva, código do clima, sensação térmica, pressão, direção e rajada do vento |
| Dados armazenados em banco | `GET /api/leituras/total` antes e depois de 35 s | ✅ 1115 → 1118 leituras |
| Java, Hibernate, orientação a objetos e DAO | Código: interfaces DAO no módulo `compartilhado` com implementações `*DAOHibernate`; sem JDBC direto | ✅ |
| Banco MySQL, SQLite ou SQL Server | MySQL (`mysql-connector-j`) | ✅ |
| Visualização em qualquer navegador | Edge no computador e em largura de celular (390 px) | ✅ |
| Coleta a cada 10 s | Intervalo entre as 15 últimas leituras `REAL` | ✅ média 10,2 s (todos os intervalos 10 s) |
| Duas aplicações distintas | `CollectorApplication` (API → banco) e `WebappApplication` (banco → navegador), módulos Maven separados | ✅ |
| Página com HTML e CSS e gráfico no domínio do tempo | Gráficos por hora e por dia na Visão geral e no Histórico | ✅ |
| Testes simulando condições meteorológicas | Coletor em modo `SIMULADO` (testes automatizados e evidência de 24/10) e tela de Simulações | ✅ |
| Tratamento de erros | Parâmetros inválidos na API e no formulário do Histórico | ✅ após a correção abaixo |

## Testes da API e do banco (44 verificações)

- **Páginas e arquivos estáticos:** `/`, `/historico`, `/simulacoes` e `/configuracoes`, além de CSS, JS e imagens, respondem 200.
- **Estação:** a estação ativa é consultada corretamente. A busca de cidades devolve sugestões, e um termo curto não quebra a busca.
- **Leituras:** última leitura, últimas N, ordem cronológica, filtros `REAL`/`SIMULADO`, consulta por período e total funcionam.
- **Histórico e previsão:** histórico diário de 7 dias, previsão de 5 dias e histórico horário do dia funcionam.
- **Erros que respondem 400 com mensagem clara:**
  - limite não numérico ou zero;
  - modo inválido;
  - período maior que 31 dias;
  - data inválida ou ausente;
  - previsão com `dias` fora de 1–16.
- **Rota inexistente:** responde 404.

**Defeito encontrado e corrigido:** `GET /api/resumos-diarios/previsoes?dias=0` (ou `99`) respondia **500**. O
`ResumoMeteorologicoController` validava o parâmetro, mas não tinha um tratador para `IllegalArgumentException`.
Agora responde **400** com `{"erro": "dias deve estar entre 1 e 16"}`. A mesma correção cobre o caso de não haver
estação ativa. Ela é coberta por `ResumoMeteorologicoControllerTest`.

## Testes de interface (54 verificações)

| Tela | O que foi testado | Resultado |
|---|---|---|
| Visão geral | Temperatura do card igual ao banco; 4 cartões de variáveis; presença de precipitação; fundo de acordo com clima/horário; previsão de 5 dias; últimas medições; gráfico em 24 h / 7 d / 30 d; atualização automática sem recarregar | ✅ |
| Histórico | Períodos 24 h / 7 d / 30 d; troca de variável (temperatura, umidade, precipitação, vento); 3 mini gráficos; resumo do período; filtro por datas | ✅ |
| Histórico (erros) | Data inicial depois da final → "Escolha um período válido."; período > 31 dias → "O período máximo é de 31 dias." | ✅ |
| Simulações | Executar os 3 cenários; última simulação e histórico atualizados; gráfico comparativo nas 4 variáveis | ✅ |
| Configurações | Modo Simulado ativo em todas as telas e retorno ao Real; salvar intervalo e observações; valores mantidos após recarregar | ✅ |
| Tema | Claro, Escuro e Automático (acompanha o sistema); botão do topo; tema mantido após recarregar | ✅ |
| Localização | Abrir o seletor; buscar "Campinas" (autocomplete); fechar com Esc e com Cancelar sem enviar alteração | ✅ |
| Celular (390 px) | As 4 telas sem rolagem lateral | ✅ |
| Console do navegador | Erros de JavaScript | ✅ nenhum |

A única mensagem no console era o `favicon.ico` ausente (404). O ícone da aba foi adicionado em `img/favicon.svg`.

Capturas de tela em [`2026-10-28/`](2026-10-28/).

## Testes automatizados

`mvn test` na raiz: **56 testes, 0 falhas, 0 erros e 0 ignorados**. São 11 no compartilhado, 30 no collector e
15 no webapp, já incluindo os 3 novos de `ResumoMeteorologicoControllerTest`.

Nenhuma senha, conteúdo de `.env` ou dump com credenciais foi registrado neste documento.
