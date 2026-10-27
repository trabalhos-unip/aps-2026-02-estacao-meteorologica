import { leiturasApi, resumosDiariosApi, resumosHorariosApi, estacaoApi } from "./api.js";
import { demoApi, demoForecast } from "./demo-data.js?v=20261026-4";
import { drawScenarioComparison } from "./charts.js?v=20261026-4";
import { drawTemporalChart, clearTemporalChart } from "./temporal-charts.js?v=20261026-4";
import { overviewPage, historyPage, simulationsPage, settingsPage, scenarios } from "./pages.js?v=20261027-2";
import { anchorDate, apiDate, dateInput, dateTime, fillTable, icon, number, parseDate, renderShell, showToast, text, time, unit, updateCollectorCard } from "./ui.js?v=20261027-2";

const path = ({ "/historico": "/historico", "/simulacoes": "/simulacoes", "/configuracoes": "/configuracoes" })[window.location.pathname.replace(/\/$/, "")] || "/";
const pages = { "/": overviewPage, "/historico": historyPage, "/simulacoes": simulationsPage, "/configuracoes": settingsPage };
renderShell(path, pages[path]());
const preferencesKey = "meteora-ui-preferences";
function savedPreferences() {
  try {
    const value = JSON.parse(localStorage.getItem(preferencesKey) || "{}");
    return value && typeof value === "object" ? value : {};
  }
  catch { return {}; }
}
let activeReadingsApi = leiturasApi;
function simulated() {
  return activeReadingsApi === demoApi;
}
function applyMode(mode) {
  activeReadingsApi = mode === "simulado" ? demoApi : leiturasApi;
  document.body.classList.toggle("simulated-mode", simulated());
  document.querySelector("#mode-badge").hidden = !simulated();
  if (simulated()) {
    text("#page-subtitle", {
      "/": "Explore condições meteorológicas simuladas",
      "/historico": "Visualize a evolução de dados simulados",
      "/simulacoes": "Exemplos de cenários meteorológicos para testes do sistema",
      "/configuracoes": "Escolha quais dados aparecem na aplicação",
    }[path]);
  }
  text("#settings-mode-note", simulated()
    ? "Modo simulado ativo: todas as telas mostram exemplos gerados neste navegador."
    : "Modo real ativo: as telas consultam as leituras da API e do MySQL.");
  text("#history-data-note", simulated()
    ? "Dados simulados gerados neste navegador para testar gráficos e histórico; não estão no MySQL."
    : "Histórico reconstituído e previsão vêm da Open-Meteo; leituras atuais são registradas separadamente no MySQL.");
}
applyMode(savedPreferences().mode);
function showLocation(preferences) {
  const city = String(preferences.city || "").trim();
  const state = String(preferences.state || "").trim();
  text("#location-label", city ? `${city}${state === "São Paulo" ? ", SP" : state ? `, ${state}` : ""}` : "Localização da estação");
}
showLocation(savedPreferences());

async function showConfiguredLocation() {
  try {
    const source = await leiturasApi.buscarFonte();
    if (source.cidade) text("#location-label", `${source.cidade}${source.estado ? `, ${source.estado}` : ""}`);
  } catch {
    // A localização configurada não impede o uso da página se a API local estiver indisponível.
  }
}
showConfiguredLocation();

const rangeDays = { "24h": 1, "7d": 7, "30d": 30 };
const variables = {
  temperatura: { title: "temperatura", suffix: "°C", color: "#2498ed" },
  umidadeRelativa: { title: "umidade relativa", suffix: "%", color: "#269ceb" },
  precipitacao: { title: "precipitação", suffix: "mm", color: "#6a95ed", style: "bar" },
  velocidadeVento: { title: "velocidade do vento", suffix: "km/h", color: "#18b979" },
};
let latestReading = null;
let historyReadings = [];
let historyStart = null;
let historyEnd = null;
let homeChartRequest = 0;
let historyRequest = 0;

function rangeDates(range, anchor = anchorDate(latestReading)) {
  const end = new Date(anchor);
  end.setHours(23, 59, 59, 999);
  const start = new Date(anchor);
  start.setHours(0, 0, 0, 0);
  start.setDate(start.getDate() - rangeDays[range] + 1);
  return [start, end];
}

function isDailyPeriod(start, end) {
  return dateInput(start) !== dateInput(end);
}

function numericAggregate(readings, field, method = "average") {
  const values = numericValues(readings, field);
  if (!values.length) return null;
  return method === "maximum" ? Math.max(...values) : values.reduce((total, value) => total + value, 0) / values.length;
}

function consolidateByDay(readings) {
  const groups = new Map();
  for (const reading of readings) {
    const day = String(reading.dataHoraColeta || "").slice(0, 10);
    if (!day) continue;
    const group = groups.get(day) || [];
    group.push(reading);
    groups.set(day, group);
  }
  return [...groups.entries()].map(([day, readingsOfDay]) => {
    const precipitationValues = readingsOfDay.map(reading => reading.precipitacao).filter(value => value !== null && value !== undefined);
    return {
      id: readingsOfDay.at(-1).id,
      dataHoraColeta: `${day}T12:00:00`,
      temperatura: numericAggregate(readingsOfDay, "temperatura"),
      umidadeRelativa: numericAggregate(readingsOfDay, "umidadeRelativa"),
      // A fonte pode repetir o mesmo acumulado durante o dia; usar o maior valor evita somar a mesma chuva a cada coleta.
      precipitacao: numericAggregate(readingsOfDay, "precipitacao", "maximum"),
      velocidadeVento: numericAggregate(readingsOfDay, "velocidadeVento"),
      presencaPrecipitacao: precipitationValues.length ? precipitationValues.some(value => Number(value) > 0) : null,
      fonteDados: [...new Set(readingsOfDay.map(reading => reading.fonteDados).filter(Boolean))].join(", "),
      diario: true,
      quantidadeLeituras: readingsOfDay.length,
    };
  });
}

function readingsForPeriod(readings, start, end) {
  return isDailyPeriod(start, end) ? consolidateByDay(readings) : readings;
}

function resumoComoLeitura(resumo) {
  const temperatura = resumo.temperaturaMedia ?? (
    resumo.temperaturaMinima != null && resumo.temperaturaMaxima != null
      ? (Number(resumo.temperaturaMinima) + Number(resumo.temperaturaMaxima)) / 2 : null
  );
  return {
    id: resumo.id,
    dataHoraColeta: `${resumo.data}T12:00:00`,
    temperatura,
    umidadeRelativa: resumo.umidadeMedia,
    precipitacao: resumo.precipitacaoTotal,
    velocidadeVento: resumo.ventoMaximo,
    codigoClima: resumo.codigoClima,
    descricaoClima: resumo.descricaoClima,
    presencaPrecipitacao: resumo.precipitacaoTotal == null ? null : Number(resumo.precipitacaoTotal) > 0,
    fonteDados: resumo.fonteDados,
    diario: true,
  };
}

function resumoHorarioComoLeitura(resumo) {
  return {
    id: resumo.id,
    dataHoraColeta: resumo.dataHora,
    temperatura: resumo.temperatura,
    umidadeRelativa: resumo.umidadeRelativa,
    precipitacao: resumo.precipitacao,
    chuva: resumo.chuva,
    velocidadeVento: resumo.velocidadeVento,
    codigoClima: resumo.codigoClima,
    descricaoClima: resumo.descricaoClima,
    presencaPrecipitacao: resumo.precipitacao == null && resumo.chuva == null ? null : Number(resumo.precipitacao || 0) > 0 || Number(resumo.chuva || 0) > 0,
    fonteDados: resumo.fonteDados,
  };
}

function chaveDaHora(reading) {
  return String(reading.dataHoraColeta || "").slice(0, 13);
}

async function buscarSerieDoPeriodo(start, end) {
  if (simulated()) {
    return activeReadingsApi.buscarPorPeriodo(apiDate(start), apiDate(end));
  }
  if (!isDailyPeriod(start, end)) {
    const [resumos, leiturasLocais] = await Promise.all([
      resumosHorariosApi.buscarHistorico(apiDate(start), apiDate(end)),
      activeReadingsApi.buscarPorPeriodo(apiDate(start), apiDate(end)),
    ]);
    // A leitura local tem prioridade na mesma hora, pois é a observação mais próxima da estação.
    const horasLocais = new Set(leiturasLocais.map(chaveDaHora));
    return [...resumos.map(resumoHorarioComoLeitura).filter(reading => !horasLocais.has(chaveDaHora(reading))), ...leiturasLocais];
  }
  const inicioHoje = new Date();
  inicioHoje.setHours(0, 0, 0, 0);
  const fimOntem = new Date(inicioHoje);
  fimOntem.setMilliseconds(-1);
  const historico = start <= fimOntem
    ? await resumosDiariosApi.buscarHistorico(dateInput(start), dateInput(new Date(Math.min(end.getTime(), fimOntem.getTime()))))
    : [];
  const leiturasDeHoje = end >= inicioHoje
    ? await activeReadingsApi.buscarPorPeriodo(apiDate(new Date(Math.max(start.getTime(), inicioHoje.getTime()))), apiDate(end))
    : [];
  return [...historico.map(resumoComoLeitura), ...leiturasDeHoje];
}

function readingMoment(reading) {
  return reading.diario
    ? new Intl.DateTimeFormat("pt-BR", { day: "2-digit", month: "2-digit", year: "numeric" }).format(parseDate(reading.dataHoraColeta))
    : time(reading.dataHoraColeta);
}

function setRangeButtons(range, ids) {
  for (const id of ids) {
    document.querySelectorAll(`#${id} [data-range]`).forEach(button => {
      const selected = button.dataset.range === range;
      button.classList.toggle("selected", selected);
      button.setAttribute("aria-pressed", String(selected));
    });
  }
}

function numericValues(readings, field) {
  return readings.map(reading => reading[field]).filter(value => value !== null && value !== undefined && Number.isFinite(Number(value))).map(Number);
}

async function fetchLatest() {
  try {
    latestReading = await activeReadingsApi.buscarUltima();
    updateCollectorCard(latestReading);
    if (simulated()) {
      text("#collector-title", "Dados simulados");
      text("#sidebar-collector-note", "Exemplos locais; o coletor não foi alterado.");
    }
    return latestReading;
  } catch (error) {
    updateCollectorCard(null);
    return null;
  }
}

function renderHomeReading(reading, recent) {
  if (!reading) {
    text("#hero-description", "Ainda não há leituras disponíveis");
    text("#current-date", "Inicie o coletor para registrar dados");
    return;
  }
  text("#hero-temperature", number(reading.temperatura));
  const isPreviousValid = latestReading && reading.id !== latestReading.id;
  text("#hero-description", `${reading.descricaoClima || "Condição meteorológica não informada"}${isPreviousValid ? " · última leitura válida" : ""}`);
  text("#current-date", `${isPreviousValid ? "Última leitura válida: " : ""}${dateTime(reading.dataHoraColeta)}`);
  text("#metric-temperature", number(reading.temperatura));
  text("#metric-humidity", number(reading.umidadeRelativa, 0));
  text("#metric-precipitation", number(reading.precipitacao));
  text("#metric-wind", number(reading.velocidadeVento));
  const first = recent.at(0);
  const temperatures = numericValues(recent, "temperatura");
  const humidity = numericValues(recent, "umidadeRelativa");
  text("#temperature-detail", temperatures.length ? `Mín. ${unit(Math.min(...temperatures), "°C")} · Máx. ${unit(Math.max(...temperatures), "°C")}` : "Sem histórico comparável");
  text("#humidity-detail", humidity.length ? `Mín. ${number(Math.min(...humidity), 0)}% · Máx. ${number(Math.max(...humidity), 0)}%` : "Sem histórico comparável");
  text("#precipitation-detail", reading.presencaPrecipitacao == null ? "Presença de chuva desconhecida" : reading.presencaPrecipitacao ? "Chuva presente" : "Sem chuva nesta leitura");
  text("#wind-detail", "Velocidade na última leitura");
  const delta = first && first.temperatura !== null && reading.temperatura !== null ? Number(reading.temperatura) - Number(first.temperatura) : null;
  text("#temperature-detail-trend", delta === null ? "Sem tendência disponível" : `${delta >= 0 ? "↗ +" : "↘ "}${number(delta)} °C nas últimas leituras`);
  text("#humidity-detail-trend", simulated() ? "Dados simulados" : "Dados da estação");
  text("#precipitation-detail-trend", reading.presencaPrecipitacao == null ? "Sem indicação disponível" : reading.presencaPrecipitacao ? "Precipitação detectada" : "Sem precipitação detectada");
  text("#wind-detail-trend", simulated() ? "Dados simulados" : "Dados da estação");
}

async function loadHomeChart(range) {
  const request = ++homeChartRequest;
  setRangeButtons(range, ["home-range"]);
  const [start, end] = rangeDates(range);
  text("#home-chart-subtitle", "Carregando período…");
  try {
    const readings = await buscarSerieDoPeriodo(start, end);
    if (request !== homeChartRequest) return;
    const chartReadings = readingsForPeriod(readings, start, end);
    const summary = drawTemporalChart("#home-chart-canvas", chartReadings, "temperatura", "°C", "#2498ed", { start, end, daily: isDailyPeriod(start, end) });
    text("#home-chart-subtitle", range === "24h"
      ? `Médias por hora · ${summary.available} de 24 horas com dados${summary.first ? ` · ${summary.first}–${summary.last}` : ""}`
      : `Médias por dia · ${summary.available} de ${summary.total} dias com dados`);
  } catch (error) {
    if (request !== homeChartRequest) return;
    clearTemporalChart("#home-chart-canvas", error.message);
    text("#home-chart-subtitle", "Dados indisponíveis");
  }
}

function renderDemoForecast() {
  if (!simulated()) return;
  text("#forecast-source", "Exemplo de previsão simulada");
  text("#forecast-note", "Previsão ilustrativa gerada neste navegador.");
  text("#overview-data-note", "Modo simulado: todos os valores desta tela são exemplos locais. Nenhum deles é gravado no MySQL.");
  document.querySelector("#home-source-chip").innerHTML = `${icon("flask")} Fonte: Simulado`;
  const rows = document.querySelector("#forecast-rows");
  rows.setAttribute("aria-label", "Previsão simulada para cinco dias");
  rows.innerHTML = demoForecast().map((item, index) => {
    const label = index === 0 ? "Hoje" : new Intl.DateTimeFormat("pt-BR", { weekday: "short" }).format(item.day).replace(".", "");
    const date = new Intl.DateTimeFormat("pt-BR", { day: "2-digit", month: "2-digit" }).format(item.day);
    return `<div class="forecast-row"><span><strong>${label}</strong><small>${date}</small></span><span class="forecast-weather">${icon(item.rain > 0 ? "rain" : "sun")}</span><span>${number(item.maximum, 0)}°</span><span>${number(item.minimum, 0)}°</span><span>${number(item.rain)} mm</span>${icon("chevron")}</div>`;
  }).join("");
}

async function renderForecast() {
  if (simulated()) {
    renderDemoForecast();
    return;
  }
  const rows = document.querySelector("#forecast-rows");
  try {
    const previsoes = await resumosDiariosApi.buscarPrevisoes(5);
    if (!previsoes.length) throw new Error("A previsão ainda não foi carregada pelo coletor.");
    text("#forecast-source", "Previsão da Open-Meteo");
    text("#forecast-note", "Atualizada pelo coletor; dias futuros não são leituras da estação.");
    rows.setAttribute("aria-label", "Previsão para os próximos cinco dias");
    rows.innerHTML = previsoes.map((item, index) => {
      const day = parseDate(`${item.data}T12:00:00`);
      const label = index === 0 ? "Hoje" : new Intl.DateTimeFormat("pt-BR", { weekday: "short" }).format(day).replace(".", "");
      const date = new Intl.DateTimeFormat("pt-BR", { day: "2-digit", month: "2-digit" }).format(day);
      const rainy = Number(item.precipitacaoTotal || 0) > 0 || [51, 53, 55, 61, 63, 65, 80, 81, 82, 95, 96, 99].includes(item.codigoClima);
      return `<div class="forecast-row"><span><strong>${label}</strong><small>${date}</small></span><span class="forecast-weather">${icon(rainy ? "rain" : "sun")}</span><span>${number(item.temperaturaMaxima, 0)}°</span><span>${number(item.temperaturaMinima, 0)}°</span><span>${number(item.precipitacaoTotal)} mm</span>${icon("chevron")}</div>`;
    }).join("");
  } catch (error) {
    text("#forecast-source", "Previsão ainda indisponível");
    text("#forecast-note", "Inicie o coletor para carregar os próximos dias.");
    rows.innerHTML = `<div class="forecast-row"><span><strong>—</strong><small>sem dados</small></span><span>—</span><span>—</span><span>—</span><span>—</span></div>`;
  }
}

async function initHome() {
  await renderForecast();
  await fetchLatest();
  try {
    const recent = await activeReadingsApi.buscarUltimas(5);
    const lastValid = [...recent].reverse().find(reading => reading.temperatura != null || reading.umidadeRelativa != null || reading.precipitacao != null || reading.velocidadeVento != null);
    renderHomeReading(lastValid || latestReading, recent);
    fillTable("#recent-rows", [...recent].reverse(), [
      { value: reading => time(reading.dataHoraColeta) },
      { value: reading => unit(reading.temperatura, "°C") },
      { value: reading => unit(reading.umidadeRelativa, "%", 0) },
      { value: reading => unit(reading.precipitacao, "mm") },
      { value: reading => unit(reading.velocidadeVento, "km/h") },
    ]);
  } catch (error) {
    renderHomeReading(latestReading, []);
    fillTable("#recent-rows", [], Array.from({ length: 5 }, () => ({})), error.message);
  }
  await loadHomeChart("7d");
  document.querySelector("#home-range").addEventListener("click", event => {
    const button = event.target.closest("[data-range]");
    if (button) loadHomeChart(button.dataset.range);
  });
}

function renderSummary(readings) {
  const values = numericValues(readings, "temperatura");
  const rows = document.querySelector("#summary-rows");
  if (!values.length) {
    rows.innerHTML = '<p class="muted">Não há temperaturas válidas neste período.</p>';
    return;
  }
  const average = values.reduce((sum, value) => sum + value, 0) / values.length;
  const minimum = Math.min(...values);
  const maximum = Math.max(...values);
  const variation = values.at(-1) - values[0];
  const minReading = readings.find(reading => reading.temperatura != null && Number(reading.temperatura) === minimum);
  const maxReading = readings.find(reading => reading.temperatura != null && Number(reading.temperatura) === maximum);
  rows.innerHTML = `
    <div class="summary-row"><span class="icon-disc red">♨</span><span>Temperatura média</span><strong>${unit(average, "°C")}</strong></div>
    <div class="summary-row"><span class="icon-disc blue">↓</span><span>Temperatura mínima</span><strong class="summary-value">${unit(minimum, "°C")}<small>${readingMoment(minReading)}</small></strong></div>
    <div class="summary-row"><span class="icon-disc red">↑</span><span>Temperatura máxima</span><strong class="summary-value">${unit(maximum, "°C")}<small>${readingMoment(maxReading)}</small></strong></div>
    <div class="summary-row"><span class="icon-disc teal">↗</span><span>Variação no período</span><strong class="${variation >= 0 ? "positive" : "negative"}">${variation >= 0 ? "+" : ""}${unit(variation, "°C")}</strong></div>`;
}

function redrawHistoryCharts() {
  const variable = document.querySelector("#history-variable").value;
  const settings = variables[variable];
  text("#history-chart-title", `Evolução da ${settings.title}`);
  const domain = { start: historyStart, end: historyEnd, daily: isDailyPeriod(historyStart, historyEnd) };
  const periodSubtitle = (summary, field) => {
    const measure = field === "precipitacao" ? "Maior valor" : "Média";
    return domain.daily
      ? `${measure} por dia · ${summary.available} de ${summary.total} dias com dados`
      : `${measure} por hora · ${summary.available} de 24 horas com dados${summary.first ? ` · ${summary.first}–${summary.last}` : ""}`;
  };
  const summary = drawTemporalChart("#history-chart-canvas", historyReadings, variable, settings.suffix, settings.color, domain);
  text("#history-chart-subtitle", periodSubtitle(summary, variable));
  const miniCharts = [
    ["#humidity-chart", "umidadeRelativa", "%", "#269ceb"],
    ["#rain-chart", "precipitacao", "mm", "#6a95ed"],
    ["#wind-chart", "velocidadeVento", "km/h", "#18b979"],
  ];
  miniCharts.forEach(([selector, field, suffix, color], index) => {
    const miniSummary = drawTemporalChart(selector, historyReadings, field, suffix, color, domain);
    document.querySelectorAll(".mini-chart-card .title-with-icon small")[index].textContent = periodSubtitle(miniSummary, field);
  });
}

function renderHistoryDetails() {
  text("#summary-period", `${dateTime(historyStart)} a ${dateTime(historyEnd)}`);
  text("#period-start", dateTime(historyStart));
  text("#period-end", dateTime(historyEnd));
  text("#period-count-label", historyReadings.at(0)?.diario ? "Dias com dados" : "Registros do dia");
  text("#period-total", number(historyReadings.length, 0));
  const sources = [...new Set(historyReadings.map(reading => reading.fonteDados).filter(Boolean))];
  text("#period-source", sources.length ? sources.join(", ") : "—");
  renderSummary(historyReadings);
  const visible = [...historyReadings].reverse().slice(0, 100);
  fillTable("#history-rows", visible, [
    { value: reading => reading.diario ? readingMoment(reading) : dateTime(reading.dataHoraColeta) },
    { value: reading => unit(reading.temperatura, "°C") },
    { value: reading => unit(reading.umidadeRelativa, "%", 0) },
    { value: reading => unit(reading.precipitacao, "mm") },
    { value: reading => unit(reading.velocidadeVento, "km/h") },
    { value: reading => reading.fonteDados || "—", badge: true },
  ]);
  redrawHistoryCharts();
}

async function loadHistory(start, end) {
  const request = ++historyRequest;
  historyStart = start;
  historyEnd = end;
  try {
    const readings = await buscarSerieDoPeriodo(start, end);
    if (request !== historyRequest) return;
    historyReadings = readingsForPeriod(readings, start, end);
    renderHistoryDetails();
    if (historyReadings.length > 100) showToast("Mostrando os 100 registros mais recentes na tabela.");
  } catch (error) {
    if (request !== historyRequest) return;
    historyReadings = [];
    renderHistoryDetails();
    showToast(error.message);
  }
}

function setHistoryPreset(range) {
  setRangeButtons(range, ["history-range", "chart-range"]);
  const [start, end] = rangeDates(range);
  document.querySelector("#history-start").value = dateInput(start);
  document.querySelector("#history-end").value = dateInput(end);
  loadHistory(start, end);
}

async function initHistory() {
  await fetchLatest();
  setHistoryPreset("7d");
  for (const id of ["history-range", "chart-range"]) {
    document.querySelector(`#${id}`).addEventListener("click", event => {
      const button = event.target.closest("[data-range]");
      if (button) setHistoryPreset(button.dataset.range);
    });
  }
  document.querySelector("#history-variable").addEventListener("change", redrawHistoryCharts);
  document.querySelector("#history-filter").addEventListener("submit", event => {
    event.preventDefault();
    const start = parseDate(`${document.querySelector("#history-start").value}T00:00:00`);
    const end = parseDate(`${document.querySelector("#history-end").value}T23:59:59`);
    if (!start || !end || start > end) return showToast("Escolha um período válido.");
    if (end - start > 31 * 24 * 60 * 60_000) return showToast("O período máximo é de 31 dias.");
    document.querySelectorAll("#history-range .segment, #chart-range .segment").forEach(button => {
      button.classList.remove("selected");
      button.setAttribute("aria-pressed", "false");
    });
    loadHistory(start, end);
  });
}

function initSimulations() {
  fetchLatest();
  drawScenarioComparison("#comparison-chart", "Temperatura");
  const executions = [];
  function renderPreview() {
    const last = executions[0];
    if (!last) return;
    const item = scenarios.find(scenario => scenario.key === last.key);
    const container = document.querySelector("#last-simulation-content");
    container.className = "simulation-result";
    container.replaceChildren();
    const head = document.createElement("div");
    head.className = "simulation-result-head";
    const heading = document.createElement("div");
    heading.innerHTML = `<span class="icon-disc ${item.tone}">${document.querySelector(`[data-scenario="${item.key}"]`).closest(".scenario-card").querySelector(".scenario-icon").innerHTML}</span><div><strong></strong><small>Prévia local em <time></time></small></div>`;
    heading.querySelector("strong").textContent = item.title;
    heading.querySelector("time").textContent = dateTime(last.at);
    const badge = document.createElement("span");badge.className="pill green-pill";badge.textContent="Prévia";
    head.append(heading,badge);container.append(head);
    const values = document.createElement("dl");
    for (const [label,value] of [["Temperatura",item.values[0]],["Umidade",item.values[1]],["Precipitação",item.values[2]],["Vento",item.values[3]]]) {
      const row=document.createElement("div"),dt=document.createElement("dt"),dd=document.createElement("dd");
      dt.textContent=label;dd.textContent=value;row.append(dt,dd);values.append(row);
    }
    container.append(values);
    const note=document.createElement("p");note.className="simulation-result-note";note.textContent="Resultado ilustrativo; não registrado no banco.";container.append(note);
    const rows=document.querySelector("#sim-history-rows");rows.replaceChildren();
    for(const execution of executions){
      const scenario=scenarios.find(candidate=>candidate.key===execution.key);
      const row=rows.insertRow();
      for(const value of [dateTime(execution.at),scenario.title,...scenario.values]){row.insertCell().textContent=value;}
    }
  }
  if (simulated()) {
    const now = Date.now();
    executions.push(
      { key: "chuva", at: new Date(now - 60 * 60_000) },
      { key: "seco", at: new Date(now - 4 * 60 * 60_000) },
      { key: "vento", at: new Date(now - 24 * 60 * 60_000) },
    );
    renderPreview();
  }
  document.querySelectorAll("[data-scenario]").forEach(button => {
    button.addEventListener("click", () => {
      executions.unshift({key:button.dataset.scenario,at:new Date()});
      renderPreview();
      showToast("Prévia local exibida. Nenhum dado foi enviado ao coletor ou gravado no MySQL.");
    });
  });
  document.querySelectorAll("[data-sim-metric]").forEach(button => {
    button.addEventListener("click", () => {
      document.querySelectorAll("[data-sim-metric]").forEach(other => other.classList.toggle("selected", other === button));
      drawScenarioComparison("#comparison-chart", button.dataset.simMetric);
    });
  });
}

async function initSettings() {
  function showLastReading() {
    text("#settings-last-reading", latestReading ? dateTime(latestReading.dataHoraColeta) : "—");
    if (!latestReading) return text("#settings-last-badge", "Indisponível");
    if (simulated()) return text("#settings-last-badge", "Exemplo");
    const minutes = Math.max(0, Math.round((Date.now() - parseDate(latestReading.dataHoraColeta).getTime()) / 60_000));
    text("#settings-last-badge", minutes < 1 ? "Agora" : `Há ${minutes} min`);
  }
  const saved = savedPreferences();
  const citySelect = document.querySelector("#settings-city");
  const stateSelect = document.querySelector("#settings-state");
  const chooseCity = (city, state = "") => {
    if (!city) return;
    const existing = [...citySelect.options].find(option => option.value === city);
    if (!existing) citySelect.add(new Option(city, city, true, true));
    citySelect.value = city;
    if (state) stateSelect.value = state;
  };
  chooseCity(saved.city, saved.state);
  stateSelect.value = saved.state || "";
  document.querySelector("#settings-interval").value = saved.interval || "900";
  document.querySelector("#settings-notes").value = saved.notes || "";
  document.querySelector(`input[name="settings-mode"][value="${saved.mode === "simulado" ? "simulado" : "real"}"]`).checked = true;
  try {
    const estacao = await estacaoApi.ativa();
    // A estação ativa é a fonte de verdade, mesmo se outro navegador alterou a cidade.
    chooseCity(estacao.cidade, estacao.estado);
  } catch {
    // A tela continua disponível para cadastrar a primeira estação.
  }
  const $ = window.jQuery;
  if ($?.fn?.select2) {
    const $state = $(stateSelect);
    $state.select2({
      width: "100%",
      placeholder: "Todos os estados",
      allowClear: true,
      minimumResultsForSearch: 0
    });
    $state.val(stateSelect.value).trigger("change.select2");
    const $city = $(citySelect);
    $city.select2({
      width: "100%",
      placeholder: citySelect.dataset.placeholder,
      minimumInputLength: 2,
      language: {
        inputTooShort: () => "Digite ao menos 2 letras",
        searching: () => "Buscando cidades…",
        noResults: () => "Nenhuma cidade brasileira encontrada"
      },
      ajax: {
        delay: 200,
        transport: (params, success, failure) => {
          estacaoApi.sugestoes(params.data.term || "", stateSelect.value)
            .then(success).catch(failure);
          return { abort() {} };
        },
        processResults: suggestions => ({
          results: suggestions.map(item => ({
            id: item.cidade,
            text: `${item.cidade} — ${item.nomeEstado || item.estado}`,
            estado: item.estado
          }))
        })
      }
    });
    $city.on("select2:select", event => {
      const state = event.params.data.estado;
      if (state) {
        stateSelect.value = state;
        $state.trigger("change.select2");
      }
    });
    stateSelect.addEventListener("change", () => $city.val(null).trigger("change"));
  }
  document.querySelectorAll('input[name="settings-mode"]').forEach(radio => {
    radio.addEventListener("change", async () => {
      if (!radio.checked) return;
      try {
        localStorage.setItem(preferencesKey, JSON.stringify({ ...savedPreferences(), mode: radio.value }));
        applyMode(radio.value);
        await fetchLatest();
        showLastReading();
        showToast(radio.value === "simulado" ? "Modo simulado ativo em todas as telas." : "Modo real ativo em todas as telas.");
      } catch {
        showToast("Não foi possível guardar o modo neste navegador.");
      }
    });
  });
  document.querySelector("#save-settings").addEventListener("click", async () => {
    const preferences = {
      city: citySelect.value.trim(),
      state: stateSelect.value,
      interval: document.querySelector("#settings-interval").value,
      mode: document.querySelector('input[name="settings-mode"]:checked').value,
      notes: document.querySelector("#settings-notes").value.trim(),
    };
    try {
      const estacao = await estacaoApi.selecionar(preferences.city, preferences.state);
      localStorage.setItem(preferencesKey, JSON.stringify(preferences));
      applyMode(preferences.mode);
      showLocation({ city: estacao.cidade, state: estacao.estado === "SP" ? "São Paulo" : estacao.estado });
      showToast("Estação atualizada. Os dados da nova cidade começam a ser carregados em instantes.");
    } catch (error) {
      showToast(error.message || "Não foi possível atualizar a estação.");
    }
  });
  await fetchLatest();
  showLastReading();
  try {
    const fonte = await leiturasApi.buscarFonte();
    const link = document.querySelector("#settings-source-link");
    link.href = fonte.urlConsulta;
    link.hidden = false;
    text("#settings-source-detail", `${fonte.nome} · ${fonte.cidade} · ${fonte.latitude}, ${fonte.longitude}`);
  } catch (error) {
    text("#settings-source-detail", "Link para a fonte indisponível");
  }
  try {
    const total = await leiturasApi.buscarTotal();
    text("#settings-db-detail", `${number(total.total, 0)} leituras disponíveis`);
    text("#settings-db-badge", "Conectado");
    document.querySelector("#settings-db-badge").classList.add("green-pill");
  } catch (error) {
    text("#settings-db-detail", error.message);
    text("#settings-db-badge", "Indisponível");
    document.querySelector("#settings-db-badge").classList.add("neutral-pill");
  }
}

({ "/": initHome, "/historico": initHistory, "/simulacoes": initSimulations, "/configuracoes": initSettings })[path]();
