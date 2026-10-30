import { icon } from "./ui.js?v=20261028-2";

function tabs(id, selected = "24h") {
  const options = id === "home-chart-range"
    ? [["24h", "Hoje"], ["7d", "7 dias"], ["15d", "15 dias"]]
    : id === "history-range"
      ? [["24h", "Hoje"], ["7d", "7 dias"], ["30d", "30 dias"]]
      : [["24h", "Hoje"], ["7d", "7 dias"], ["30d", "30 dias"]];
  return `<div class="segmented" id="${id}" role="group" aria-label="Período do gráfico">
    ${options.map(([value, label]) =>
      `<button type="button" class="segment ${value === selected ? "selected" : ""}" data-range="${value}" aria-pressed="${value === selected}">${label}</button>`).join("")}
  </div>`;
}

const METEOCONS_STATIC = "https://cdn.meteocons.com/3.0.0-next.10/svg-static/fill";

function meteocon(asset, className = "metric-meteocon") {
  return `<img class="${className}" src="${METEOCONS_STATIC}/${asset}.svg" alt="" aria-hidden="true" decoding="async">`;
}

function metricCard(label, asset, id, suffix, detailId) {
  return `<article class="card metric-card">
    <div class="metric-heading">${meteocon(asset)}<strong>${label}</strong></div>
    <p class="metric-reading"><span id="${id}">—</span><span class="metric-suffix">${suffix}</span></p>
    <p class="metric-detail" id="${detailId}">Aguardando leitura</p>
    <p class="metric-trend" id="${detailId}-trend">—</p>
  </article>`;
}

function miniSeriesLegend(id) {
  return `<div class="mini-series-legend" id="${id}" aria-label="Legenda das séries" hidden><span class="measured"><i></i>Medido</span><span class="forecast"><i></i>Previsão</span></div>`;
}

function forecastRows() {
  return Array.from({ length: 5 }, (_, index) => {
    const date = new Date();
    date.setDate(date.getDate() + index);
    const weekday = new Intl.DateTimeFormat("pt-BR", { weekday: "long" }).format(date);
    const label = index === 0 ? "Hoje" : `${weekday.charAt(0).toLocaleUpperCase("pt-BR")}${weekday.slice(1)}`;
    const compactDate = new Intl.DateTimeFormat("pt-BR", { day: "2-digit", month: "2-digit" }).format(date);
    return `<div class="forecast-row"><span><strong>${label}</strong><small>${compactDate}</small></span><span class="forecast-weather">${meteocon("overcast-day", "forecast-meteocon")}</span><span class="forecast-rain">—</span><span class="forecast-temperatures"><strong>—°</strong><small>—°</small></span></div>`;
  }).join("");
}

export function overviewPage() {
  return `<div class="overview-grid"><div class="overview-left">
    <section class="weather-hero" aria-labelledby="current-title">
      <div class="hero-head"><div><h2 id="current-title">CONDIÇÕES ATUAIS</h2><p id="current-date">Aguardando leitura…</p></div><span class="source-chip" id="home-source-chip">${meteocon("overcast-day", "weather-library-icon")} Fonte: Open-Meteo</span></div>
      <p class="hero-temperature"><span id="hero-temperature">—</span><span>°C</span></p>
      <p class="hero-description" id="hero-description">Aguardando dados da estação</p>
    </section>
    <section class="metrics-grid" aria-label="Variáveis meteorológicas">
      ${metricCard("Temperatura", "thermometer", "metric-temperature", "°C", "temperature-detail")}
      ${metricCard("Umidade", "humidity", "metric-humidity", "%", "humidity-detail")}
      ${metricCard("Chuva", "rain", "metric-rain-chance", "%", "rain-detail")}
      ${metricCard("Vento", "wind", "metric-wind", "km/h", "wind-detail")}
    </section>
    <section class="card chart-card home-chart hourly-weather-chart" aria-label="Previsão do tempo">
      <div class="section-head home-chart-toolbar"><div class="title-with-icon">${meteocon("partly-cloudy-day", "forecast-meteocon home-forecast-title-icon")}<h2 id="hourly-weather-title">Previsão do tempo · Hoje</h2></div>${tabs("home-chart-range")}</div>
      <div class="hourly-weather-chart-canvas" id="hourly-weather-rows" aria-label="Carregando previsão por hora"><div class="chart-empty">Carregando previsão por hora…</div></div>
      <div class="home-daily-forecast" id="home-daily-forecast" hidden aria-label="Carregando previsão dos próximos dias">
        <div class="home-daily-forecast-scroll" id="home-daily-forecast-scroll"></div>
      </div>
    </section>
    <section class="home-insights" aria-label="Evolução das variáveis medidas hoje">
      <article class="card mini-chart-card home-insight-card"><div class="title-with-icon">${meteocon("humidity", "mini-weather-icon")}<div><h3>Umidade relativa</h3><small id="home-humidity-subtitle">Umidade relativa do ar</small></div>${miniSeriesLegend("home-humidity-series-legend")}</div><div class="chart-canvas small" id="home-humidity-chart"><div class="chart-empty">Carregando leituras…</div></div></article>
      <article class="card mini-chart-card home-insight-card"><div class="title-with-icon">${meteocon("rain", "mini-weather-icon")}<div><h3>Precipitação</h3><small id="home-rain-subtitle">Chuva registrada hoje</small></div>${miniSeriesLegend("home-rain-series-legend")}</div><div class="chart-canvas small" id="home-rain-chart"><div class="chart-empty">Carregando leituras…</div></div></article>
      <article class="card mini-chart-card home-insight-card"><div class="title-with-icon">${meteocon("wind", "mini-weather-icon")}<div><h3>Velocidade do vento</h3><small id="home-wind-subtitle">Vento registrado hoje</small></div>${miniSeriesLegend("home-wind-series-legend")}</div><div class="chart-canvas small" id="home-wind-chart"><div class="chart-empty">Carregando leituras…</div></div></article>
    </section></div><div class="overview-right">
    <aside class="card forecast-card" aria-labelledby="forecast-title">
      <div class="card-title">${icon("calendar")}<div><h2 id="forecast-title">Previsão dos próximos dias</h2><small id="forecast-source">Dados da API Open-Meteo</small></div></div>
      <div class="forecast-rows" id="forecast-rows" aria-label="Previsão aguardando integração">${forecastRows()}</div>
      <p class="forecast-note" id="forecast-note">Previsão ainda não integrada; valores indisponíveis.</p>
    </aside>
    <section class="card recent-card" aria-labelledby="recent-title">
      <div class="section-head"><div class="title-with-icon">${icon("database")}<h2 id="recent-title">Últimas medições</h2></div><a class="secondary-button recent-see-all" href="/historico">Ver tudo ${icon("arrow")}</a></div>
      <div class="table-scroll"><table><caption class="sr-only">Últimas medições da estação</caption><thead><tr><th>Horário</th><th>Temp.</th><th>Umidade</th><th>Precip.</th><th>Vento</th></tr></thead><tbody id="recent-rows"><tr><td colspan="5" class="empty-cell">Carregando…</td></tr></tbody></table></div>
      <nav class="table-pagination recent-pagination" id="recent-pagination" aria-label="Paginação das últimas medições" hidden><button type="button" class="secondary-button" data-recent-page="previous">Anterior</button><span id="recent-page-label"></span><button type="button" class="secondary-button" data-recent-page="next">Próxima</button></nav>
    </section>
    </div>
  </div>`;
}

export function historyPage() {
  return `<form class="card filter-bar" id="history-filter">
    <div class="field period-field"><label>Período</label>${tabs("history-range")}</div>
    <label class="field">Variável<select id="history-variable"><option value="temperatura">Temperatura</option><option value="umidadeRelativa">Umidade</option><option value="precipitacao">Precipitação</option><option value="velocidadeVento">Vento</option></select></label>
  </form>
  <div class="history-grid">
    <div class="history-charts">
      <section class="card chart-card primary-history-chart">
        <div class="section-head"><div class="title-with-icon"><span class="title-icon blue-text">${icon("chart")}</span><div><h2 id="history-chart-title">Histórico de temperatura</h2><small id="history-chart-subtitle">Carregando período…</small></div></div><div class="history-chart-actions"><div class="history-series-legend" id="history-series-legend" aria-label="Legenda das séries" hidden><span class="measured"><i></i>Medido</span><span class="forecast"><i></i>Previsão</span></div></div></div>
        <div class="chart-canvas" id="history-chart-canvas"><div class="chart-empty">Carregando leituras…</div></div>
      </section>
      <div class="history-insights">
        <section class="card mini-chart-card"><div class="title-with-icon">${meteocon("humidity", "mini-weather-icon")}<div><h3>Umidade relativa</h3><small>Carregando…</small></div>${miniSeriesLegend("humidity-series-legend")}</div><div class="chart-canvas small" id="humidity-chart"></div></section>
        <section class="card mini-chart-card"><div class="title-with-icon">${meteocon("rain", "mini-weather-icon")}<div><h3>Precipitação</h3><small>Carregando…</small></div>${miniSeriesLegend("rain-series-legend")}</div><div class="chart-canvas small" id="rain-chart"></div></section>
        <section class="card mini-chart-card"><div class="title-with-icon">${meteocon("wind", "mini-weather-icon")}<div><h3>Velocidade do vento</h3><small>Carregando…</small></div>${miniSeriesLegend("wind-series-legend")}</div><div class="chart-canvas small" id="wind-chart"></div></section>
      </div>
    </div>
    <aside class="history-side">
      <section class="card summary-card"><div class="card-title"><div><h2>Resumo do período</h2><small id="summary-period">—</small></div></div><div id="summary-rows" class="summary-rows"><p class="muted">Aguardando leituras…</p></div></section>
      <section class="card historical-weather-card" aria-labelledby="historical-weather-title"><div class="card-title">${icon("calendar")}<div><h2 id="historical-weather-title">Clima dos últimos dias</h2><small>Condições já medidas</small></div></div><div class="historical-weather-list" id="historical-weather-list"><div class="chart-empty">Carregando condições medidas…</div></div></section>
    </aside>
  </div>
  <section class="card records-card history-records-card">
    <div class="section-head"><div class="title-with-icon"><span class="blue-text">${icon("database")}</span><h2>Registros no período</h2></div><small id="history-page-status" aria-live="polite"></small></div>
    <div class="table-scroll"><table><caption class="sr-only">Registros meteorológicos do período</caption><thead><tr><th>Data</th><th>Temperatura</th><th>Umidade</th><th>Precipitação</th><th>Vento</th><th>Origem</th></tr></thead><tbody id="history-rows"><tr><td colspan="6" class="empty-cell">Carregando…</td></tr></tbody></table></div>
    <nav class="table-pagination" id="history-pagination" aria-label="Paginação dos registros" hidden><button type="button" class="secondary-button" data-history-page="previous">Anterior</button><span id="history-page-label"></span><button type="button" class="secondary-button" data-history-page="next">Próxima</button></nav>
  </section>`;
}

export function settingsPage() {
  return `<section class="settings-main">
    <div class="settings-board">
      <section class="settings-column station-settings" aria-labelledby="station-settings-title">
        <section class="card settings-block settings-location-card" id="localizacao"><div class="settings-block-heading">${icon("pin")}<div><h3>Localização da estação</h3><p>Digite para pesquisar e selecionar uma cidade brasileira.</p></div></div><label class="settings-city-label" for="settings-location-city">Cidade<select id="settings-location-city" data-placeholder="Digite o nome da cidade" aria-label="Buscar cidade brasileira"><option value=""></option></select></label><div class="station-location-fields"><label>Latitude<output data-settings-latitude>—</output></label><label>Longitude<output data-settings-longitude>—</output></label></div></section>
        <section class="card settings-block settings-interval"><div class="settings-block-heading">${icon("clock")}<div><h3>Intervalo de coleta</h3><p>Referência visual; não altera o coletor Java.</p></div></div><select id="settings-interval" class="sr-only"><option value="10">10 segundos (teste APS)</option><option value="60">1 minuto</option><option value="300">5 minutos</option><option value="900">15 minutos</option><option value="1800">30 minutos</option></select><div class="interval-options" role="group" aria-label="Intervalo de coleta"><button type="button" data-interval-choice="10">10 s</button><button type="button" data-interval-choice="60">1 min</button><button type="button" data-interval-choice="300">5 min</button><button type="button" data-interval-choice="900">15 min</button><button type="button" data-interval-choice="1800">30 min</button></div></section>
      </section>
      <section class="settings-column preference-settings" aria-labelledby="preference-settings-title">
        <section class="card settings-block settings-operation"><div class="settings-block-heading">${icon("flask")}<div><h3>Modo de operação</h3><p>Escolha os dados exibidos em todas as telas.</p></div></div><div class="mode-options"><label class="mode-option"><input type="radio" name="settings-mode" value="real" checked>${meteocon("overcast-day", "mode-weather-icon")}<div><strong>Real (API)</strong><small>Leituras recebidas e armazenadas pelo coletor.</small></div></label><label class="mode-option"><input type="radio" name="settings-mode" value="simulado">${icon("flask")}<div><strong>Simulado</strong><small>Exemplos completos de 30 dias, sem gravar no banco.</small></div></label></div><p id="settings-mode-note">O modo escolhido aparece em toda a aplicação.</p></section>
        <section class="card settings-block settings-appearance"><div class="settings-block-heading">${icon("sun")}<div><h3>Aparência</h3><p>Escolha o tema visual da aplicação.</p></div></div><div class="mode-options theme-options"><label class="mode-option theme-option"><input type="radio" name="settings-theme" value="auto"><span class="theme-preview theme-preview-auto">${icon("monitor")}</span><div><strong>Automático</strong><small>Segue o sistema.</small></div></label><label class="mode-option theme-option"><input type="radio" name="settings-theme" value="claro"><span class="theme-preview theme-preview-light"><img src="/img/preview-configuracoes-claro.svg" alt="Prévia da interface clara"></span><div><strong>Claro</strong><small>Interface clara.</small></div></label><label class="mode-option theme-option"><input type="radio" name="settings-theme" value="escuro"><span class="theme-preview theme-preview-dark"><img src="/img/preview-configuracoes-escuro.svg" alt="Prévia da interface escura"></span><div><strong>Escuro</strong><small>Interface escura.</small></div></label></div></section>
      </section>
    </div>
    <footer class="settings-actions"><p class="settings-note">As preferências ficam salvas neste navegador. A configuração real do coletor permanece no <code>.env</code>.</p><button class="primary-button save-settings" id="save-settings" type="button">${icon("file")} Salvar alterações</button></footer>
  </section>`;
}
