// grafico.js — gráfico temporal das variáveis ambientais (responsabilidade do front-end).
//
// Recebe a lista devolvida por GET /api/leituras?limite=N, JÁ em ordem cronológica (mais antiga → mais recente).
// Cada item tem, entre outros campos:
//   dataHoraColeta  "2026-10-20T22:20:05"   → eixo X (tempo)
//   temperatura     14.0                    → °C
//   umidadeRelativa 89                      → %
//   precipitacao    0.0                     → mm
//   velocidadeVento 9.8                     → km/h
//   presencaPrecipitacao false              → sim/não (null = desconhecido)
//   modoOperacao    "REAL" | "SIMULADO"     → origem da leitura
//   latitude / longitude -23.514938 / -46.610504 → localização dos dados
// Valores ausentes chegam como null.
// Para separar leituras reais e simuladas: GET /api/leituras?limite=N&modo=REAL (ou SIMULADO).
//
// Sugestão: Chart.js (gráfico de linha), um gráfico por variável ou eixos separados para preservar as unidades.

function desenharGrafico(leituras) {
  const area = document.getElementById('grafico');
  area.textContent = leituras.length === 0
    ? 'Ainda não há leituras para exibir no gráfico.'
    : leituras.length + ' leituras disponíveis para o gráfico (implementação pendente).';
}
