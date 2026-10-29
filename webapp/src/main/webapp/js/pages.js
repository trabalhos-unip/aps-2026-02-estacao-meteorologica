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

function scenarioMeteoconAsset(key) {
  return { seco: "partly-cloudy-day", chuva: "rain", vento: "wind" }[key] || "overcast-day";
}

function metricCard(label, asset, id, suffix, detailId) {
  return `<article class="card metric-card">
    <div class="metric-heading">${meteocon(asset)}<strong>${label}</strong></div>
    <p class="metric-reading"><span id="${id}">—</span><span class="metric-suffix">${suffix}</span></p>
    <p class="metric-detail" id="${detailId}">Aguardando leitura</p>
    <p class="metric-trend" id="${detailId}-trend">—</p>
  </article>`;
}

function forecastRows() {
  return Array.from({ length: 5 }, (_, index) => {
    const date = new Date();
    date.setDate(date.getDate() + index);
    const label = index === 0 ? "Hoje" : new Intl.DateTimeFormat("pt-BR", { weekday: "short" }).format(date).replace(".", "");
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
      <div class="section-head home-chart-toolbar"><div class="title-with-icon"><span class="title-icon blue-text">${icon("chart")}</span><h2 id="hourly-weather-title">Previsão do tempo · Hoje</h2></div>${tabs("home-chart-range")}</div>
      <div class="hourly-weather-chart-canvas" id="hourly-weather-rows" aria-label="Carregando previsão por hora"><div class="chart-empty">Carregando previsão por hora…</div></div>
      <div class="home-daily-forecast" id="home-daily-forecast" hidden aria-label="Carregando previsão dos próximos dias">
        <div class="home-daily-forecast-scroll" id="home-daily-forecast-scroll"></div>
      </div>
    </section></div><div class="overview-right">
    <aside class="card forecast-card" aria-labelledby="forecast-title">
      <div class="card-title">${icon("calendar")}<div><h2 id="forecast-title">Previsão dos próximos dias</h2><small id="forecast-source">Dados da API Open-Meteo</small></div></div>
      <div class="forecast-rows" id="forecast-rows" aria-label="Previsão aguardando integração">${forecastRows()}</div>
      <p class="forecast-note" id="forecast-note">Previsão ainda não integrada; valores indisponíveis.</p>
    </aside>
    <section class="card recent-card" aria-labelledby="recent-title">
      <div class="section-head"><div class="title-with-icon">${icon("database")}<h2 id="recent-title">Últimas medições</h2></div><a class="text-link" href="/historico">Ver histórico ${icon("arrow")}</a></div>
      <div class="table-scroll"><table><caption class="sr-only">Últimas medições da estação</caption><thead><tr><th>Horário</th><th>Temp.</th><th>Umidade</th><th>Precip.</th><th>Vento</th></tr></thead><tbody id="recent-rows"><tr><td colspan="5" class="empty-cell">Carregando…</td></tr></tbody></table></div>
      <nav class="table-pagination recent-pagination" id="recent-pagination" aria-label="Paginação das últimas medições" hidden><button type="button" class="secondary-button" data-recent-page="previous">Anterior</button><span id="recent-page-label"></span><button type="button" class="secondary-button" data-recent-page="next">Próxima</button></nav>
    </section>
    <div class="info-strip overview-info">${icon("info")}<span id="overview-data-note">Os dados apresentados são coletados da API <a href="https://open-meteo.com/" target="_blank" rel="noreferrer">Open-Meteo</a> e armazenados no MySQL. A previsão dos próximos dias depende de uma integração adicional.</span></div>
    </div>
  </div>`;
}

export function historyPage() {
  return `<form class="card filter-bar" id="history-filter">
    <div class="field period-field"><label>Período</label>${tabs("history-range")}</div>
    <label class="field">Variável<select id="history-variable"><option value="temperatura">Temperatura</option><option value="umidadeRelativa">Umidade</option><option value="precipitacao">Precipitação</option><option value="velocidadeVento">Vento</option></select></label>
    <label class="field">Data inicial<input type="date" id="history-start" required></label>
    <label class="field">Data final<input type="date" id="history-end" required></label>
    <button class="primary-button apply-button" type="submit">Aplicar</button>
  </form>
  <div class="history-grid">
    <div class="history-charts">
      <section class="card chart-card primary-history-chart">
        <div class="section-head"><div class="title-with-icon"><span class="title-icon blue-text">${icon("chart")}</span><div><h2 id="history-chart-title">Histórico de temperatura</h2><small id="history-chart-subtitle">Carregando período…</small></div></div><div class="history-chart-actions"><div class="history-series-legend" id="history-series-legend" aria-label="Legenda das séries" hidden><span class="measured"><i></i>Medido</span><span class="forecast"><i></i>Previsão</span></div>${tabs("chart-range")}</div></div>
        <div class="chart-canvas" id="history-chart-canvas"><div class="chart-empty">Carregando leituras…</div></div>
      </section>
      <div class="history-insights">
        <section class="card mini-chart-card"><div class="title-with-icon">${meteocon("humidity", "mini-weather-icon")}<div><h3>Umidade relativa</h3><small>Últimas 24 horas</small></div></div><div class="chart-canvas small" id="humidity-chart"></div></section>
        <section class="card mini-chart-card"><div class="title-with-icon">${meteocon("rain", "mini-weather-icon")}<div><h3>Precipitação</h3><small>Últimas 24 horas</small></div></div><div class="chart-canvas small" id="rain-chart"></div></section>
        <section class="card mini-chart-card"><div class="title-with-icon">${meteocon("wind", "mini-weather-icon")}<div><h3>Velocidade do vento</h3><small>Últimas 24 horas</small></div></div><div class="chart-canvas small" id="wind-chart"></div></section>
      </div>
    </div>
    <aside class="history-side">
      <section class="card summary-card"><div class="card-title"><div><h2>Resumo do período</h2><small id="summary-period">—</small></div></div><div id="summary-rows" class="summary-rows"><p class="muted">Aguardando leituras…</p></div></section>
    </aside>
  </div>
  <section class="card records-card history-records-card">
    <div class="section-head"><div class="title-with-icon"><span class="blue-text">${icon("database")}</span><h2>Registros no período</h2></div><small id="history-page-status" aria-live="polite"></small></div>
    <div class="table-scroll"><table><caption class="sr-only">Registros meteorológicos do período</caption><thead><tr><th>Data</th><th>Temperatura</th><th>Umidade</th><th>Precipitação</th><th>Vento</th><th>Origem</th></tr></thead><tbody id="history-rows"><tr><td colspan="6" class="empty-cell">Carregando…</td></tr></tbody></table></div>
    <nav class="table-pagination" id="history-pagination" aria-label="Paginação dos registros" hidden><button type="button" class="secondary-button" data-history-page="previous">Anterior</button><span id="history-page-label"></span><button type="button" class="secondary-button" data-history-page="next">Próxima</button></nav>
  </section>`;
}

export const scenarios = [
  { key: "seco", title: "Tempo seco", desc: "Temperatura elevada, umidade reduzida e sem precipitação.", icon: "sun", tone: "amber", values: ["32,0 °C", "30%", "0,0 mm", "10,0 km/h"] },
  { key: "chuva", title: "Chuva intensa", desc: "Precipitação presente, umidade alta e temperatura menor.", icon: "rain", tone: "blue", values: ["20,0 °C", "90%", "15,0 mm", "18,0 km/h"] },
  { key: "vento", title: "Vento forte", desc: "Velocidade do vento elevada e registro da ocorrência.", icon: "wind", tone: "teal", values: ["24,0 °C", "60%", "0,0 mm", "50,0 km/h"] },
];

export function simulationsPage() {
  return `<div class="sim-grid">
    <div class="sim-main">
      <section class="scenario-hero"><div><h2>Teste cenários meteorológicos</h2><p>Execute simulações para verificar a coleta, validação e o armazenamento dos dados.</p></div><span class="source-chip">${icon("flask")} Modo de simulação</span></section>
      <div class="scenario-grid">
        ${scenarios.map(item => `<article class="card scenario-card ${item.tone}"><div class="scenario-head"><span class="scenario-icon">${meteocon(scenarioMeteoconAsset(item.key), "scenario-meteocon")}</span><div><h3>${item.title}</h3><p>${item.desc}</p></div></div><dl>
          <div><dt>${meteocon("thermometer", "scenario-detail-meteocon")}<span class="sr-only">Temperatura</span></dt><dd>${item.values[0]}</dd></div>
          <div><dt>${meteocon("humidity", "scenario-detail-meteocon")}<span class="sr-only">Umidade</span></dt><dd>${item.values[1]}</dd></div>
          <div><dt>${meteocon("rain", "scenario-detail-meteocon")}<span class="sr-only">Precipitação</span></dt><dd>${item.values[2]}</dd></div>
          <div><dt>${meteocon("wind", "scenario-detail-meteocon")}<span class="sr-only">Vento</span></dt><dd>${item.values[3]}</dd></div>
        </dl><button class="primary-button scenario-button" type="button" data-scenario="${item.key}">${icon("play")} Executar cenário</button></article>`).join("")}
      </div>
      <section class="card comparison-card"><div class="section-head"><div class="title-with-icon"><span class="title-icon blue-text">${icon("chart")}</span><div><h2>Comparação dos cenários</h2><small>Prévia ilustrativa dos cenários · não gravada</small></div></div></div><div class="scenario-metric-tabs" role="group" aria-label="Variável da comparação"><button class="selected" type="button" data-sim-metric="Temperatura">Temperatura</button><button type="button" data-sim-metric="Umidade">Umidade</button><button type="button" data-sim-metric="Precipitação">Precipitação</button><button type="button" data-sim-metric="Vento">Vento</button></div><div class="scenario-chart" id="comparison-chart"></div><div class="scenario-legend"><span><i class="amber-dot"></i>Tempo seco</span><span><i class="blue-dot"></i>Chuva intensa</span><span><i class="teal-dot"></i>Vento forte</span></div></section>
    </div>
    <aside class="sim-aside">
      <section class="card last-simulation"><div class="card-title">${icon("clock")}<h2>Última simulação</h2></div><div id="last-simulation-content" class="empty-feature"><span class="icon-disc teal">${icon("flask")}</span><strong>Nenhuma execução registrada</strong><p>Selecione um cenário para ver uma prévia local. A API ainda não grava simulações.</p></div></section>
      <section class="card sim-history"><div class="section-head"><div class="title-with-icon">${icon("clock")}<h2>Histórico de simulações</h2></div><span class="muted local-label">Nesta página</span></div><div class="table-scroll"><table><caption class="sr-only">Prévias locais de simulação nesta página</caption><thead><tr><th>Data e hora</th><th>Cenário</th><th>Temperatura</th><th>Umidade</th><th>Precip.</th><th>Vento</th></tr></thead><tbody id="sim-history-rows"><tr><td colspan="6" class="empty-cell">Nenhuma prévia nesta página.</td></tr></tbody></table></div></section>
      <div class="info-strip sim-info">${icon("info")}<span>A comparação é uma prévia visual. Os cenários testados nesta página não são enviados ao coletor nem gravados no MySQL; execução real depende da integração com o back-end e o banco.</span></div>
    </aside>
  </div>`;
}

export function settingsPage() {
  return `<div class="settings-grid">
    <section class="card settings-main">
      <div class="settings-title"><span class="icon-disc blue">${icon("settings")}</span><div><h2>Configuração da estação</h2><p>Localização, intervalo de coleta e modo de operação da estação meteorológica.</p></div></div>
      <div class="settings-section" id="localizacao"><span class="icon-disc pale">${icon("pin")}</span><div><h3>Localização da estação</h3><p>A cidade atual é exibida no topo. Abra o seletor para pesquisar uma nova cidade brasileira; estado e coordenadas são identificados automaticamente.</p><button type="button" class="secondary-button location-settings-button" data-open-location-modal>${icon("search")} Alterar cidade</button></div></div>
      <div class="settings-section"><span class="icon-disc pale">${icon("clock")}</span><div><h3>Intervalo de coleta</h3><p>Referência visual; não altera o coletor Java.</p><div class="settings-fields"><label>Intervalo<select id="settings-interval"><option value="10">10 segundos (teste APS)</option><option value="60">1 minuto</option><option value="300">5 minutos</option><option value="900">15 minutos</option></select></label></div></div></div>
      <div class="settings-section"><span class="icon-disc pale">${icon("flask")}</span><div><h3>Modo de operação</h3><p>Escolha os dados exibidos em todas as telas.</p><div class="mode-options"><label class="mode-option"><input type="radio" name="settings-mode" value="real" checked>${meteocon("overcast-day", "mode-weather-icon")}<div><strong>Real (API)</strong><small>Leituras recebidas e armazenadas pelo coletor.</small></div></label><label class="mode-option"><input type="radio" name="settings-mode" value="simulado">${icon("flask")}<div><strong>Simulado</strong><small>Exemplos completos de 30 dias, sem gravar no banco.</small></div></label></div><p id="settings-mode-note">O modo escolhido aparece em toda a aplicação.</p></div></div>
      <div class="settings-section"><span class="icon-disc pale">${icon("moon")}</span><div><h3>Aparência</h3><p>Tema da interface; fica guardado neste navegador.</p><div class="mode-options theme-options"><label class="mode-option"><input type="radio" name="settings-theme" value="auto">${icon("monitor")}<div><strong>Automático</strong><small>Segue o tema do sistema.</small></div></label><label class="mode-option"><input type="radio" name="settings-theme" value="claro">${icon("sun")}<div><strong>Claro</strong><small>Fundo claro.</small></div></label><label class="mode-option"><input type="radio" name="settings-theme" value="escuro">${icon("moon")}<div><strong>Escuro</strong><small>Fundo preto.</small></div></label></div></div></div>
      <div class="settings-section"><span class="icon-disc pale">${icon("file")}</span><div><h3>Observações do ambiente</h3><p>Informações sobre o local da estação ou condições relevantes.</p><textarea id="settings-notes" rows="3" maxlength="500" placeholder="Adicione uma observação local."></textarea></div></div>
      <button class="primary-button save-settings" id="save-settings" type="button">${icon("file")} Salvar configurações</button>
      <p class="settings-note">O modo é aplicado ao selecioná-lo e salvo neste navegador. A configuração real do coletor fica no <code>.env</code>.</p>
    </section>
    <aside class="settings-aside">
      <section class="card technical-summary"><div class="card-title">${icon("file")}<div><h2>Resumo técnico</h2><small>Informações sobre os componentes e configurações do sistema</small></div></div><div class="tech-row"><span class="icon-disc pale">${meteocon("overcast-day", "tech-weather-icon")}</span><div><strong>Fonte de dados meteorológicos</strong><small id="settings-source-detail">Consultando configuração…</small><a id="settings-source-link" class="text-link" href="#" target="_blank" rel="noreferrer" hidden>Abrir dados reais da Open-Meteo ${icon("arrow")}</a></div><span class="pill blue-pill">Open-Meteo</span></div><div class="tech-row"><span class="icon-disc pale">${icon("database")}</span><div><strong>Armazenamento dos dados</strong><small>MySQL (banco de dados)</small></div><span class="pill blue-pill">MySQL</span></div><div class="tech-row"><span class="icon-disc pale">${icon("code")}</span><div><strong>Back-end da aplicação</strong><small>Java (coletor e processamento)</small></div><span class="pill purple-pill">Java</span></div><div class="tech-row"><span class="icon-disc pale">${icon("clock")}</span><div><strong>Última leitura disponível</strong><small id="settings-last-reading">—</small></div><span class="pill blue-pill" id="settings-last-badge">—</span></div><div class="tech-row"><span class="icon-disc pale">${icon("clock")}</span><div><strong>Próxima coleta</strong><small>O coletor não informa agendamento</small></div><span class="pill neutral-pill">Não verificada</span></div></section>
      <section class="card integration-card"><div class="card-title">${icon("refresh")}<div><h2>Status da integração</h2><small>Situação verificável dos serviços e da coleta de dados</small></div></div><div class="tech-row"><span class="icon-disc pale">${icon("database")}</span><div><strong>Consulta ao banco</strong><small id="settings-db-detail">Verificando…</small></div><span class="pill" id="settings-db-badge">—</span></div><div class="tech-row"><span class="icon-disc pale">${meteocon("overcast-day", "tech-weather-icon")}</span><div><strong>Coletor de dados</strong><small>Sem endpoint de status</small></div><span class="pill neutral-pill">Não verificado</span></div><div class="tech-row"><span class="icon-disc pale">${meteocon("overcast-day", "tech-weather-icon")}</span><div><strong>Open-Meteo</strong><small>Sem teste de conexão na API web</small></div><span class="pill neutral-pill">Não verificado</span></div><div class="tech-row"><span class="icon-disc pale">${icon("check")}</span><div><strong>Validação dos dados</strong><small>Não exposta pela API web</small></div><span class="pill neutral-pill">Não verificada</span></div></section>
      <section class="card about-card"><div class="card-title">${icon("info")}<h2>Sobre esta aplicação</h2></div><p>O Meteora foi desenvolvido para a APS 2026/2, utilizando a API Open-Meteo, MySQL e Java. Esta versão apresenta coleta e análise dos dados meteorológicos disponíveis.</p></section>
    </aside>
  </div>`;
}
