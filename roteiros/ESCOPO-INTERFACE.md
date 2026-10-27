# Meteora: telas e integração

Os quatro wireframes em `../../sources/wireframes/` são referências visuais. A aplicação usa as rotas `/`, `/historico`, `/simulacoes` e `/configuracoes`. Sidebar, cabeçalho, ícones, cartões, tabelas e estilos responsivos são compartilhados.

| Tela | Já funciona com o back-end atual | Depende de integração adicional |
| --- | --- | --- |
| Visão geral | Última leitura válida, quatro variáveis, gráfico temporal e últimas medições | Previsão futura, cidade/coordenadas apresentadas pela API e status real do processo coletor |
| Histórico | Filtro por período (até 31 dias), seleção de variável, gráficos, tabela e exportação CSV | Filtro real/simulado, paginação ou agregação para períodos com muitos registros |
| Simulações | Três cenários de exemplo, comparação gráfica personalizada e prévias executáveis na página aberta, sem gravação | Executar cenários pelo coletor, registrar o modo no banco e consultar histórico/comparação persistidos das simulações |
| Configurações | Resumo técnico, horário da última leitura, verificação da consulta ao banco e preferências locais de cidade, intervalo, modo e observações salvas no navegador | Consultar e gravar configuração real da estação; consultar saúde do coletor e da API |

O menu de perfil segue o desenho do wireframe apenas como identificação visual. **Login e autenticação não fazem parte deste escopo.** O nome e a cidade mostrados no wireframe não são tratados como dados confiáveis da estação.

Os pontos pendentes são identificados na interface. Uma prévia de simulação não altera o coletor e não grava no MySQL; o histórico dessa tela vale somente até recarregar a página. A tela de configurações salva apenas preferências no `localStorage` do navegador, sem alterar o arquivo `.env`, o processo Java ou o banco. Os arquivos sincronizados em `sources/` não são modificados.
