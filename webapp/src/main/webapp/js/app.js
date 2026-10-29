import { leiturasApi, resumosDiariosApi, resumosHorariosApi, estacaoApi } from "./api.js?v=20261028-6";
import { demoApi } from "./demo-data.js?v=20261026-4";
import { drawScenarioComparison } from "./charts.js?v=20261028-1";
import { drawTemporalChart, clearTemporalChart } from "./temporal-charts.js?v=20261028-3";
import { overviewPage, historyPage, simulationsPage, settingsPage, scenarios } from "./pages.js?v=20261028-7";
import { anchorDate, apiDate, dateInput, dateTime, fillTable, icon, number, parseDate, renderShell, showToast, text, time, unit, updateCollectorCard } from "./ui.js?v=20261028-1";

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
// Tema: "auto" segue o sistema; "claro"/"escuro" ficam fixos. O index.html já aplica antes da pintura.
const systemDark = window.matchMedia("(prefers-color-scheme: dark)");
function themeChoice() {
  const theme = savedPreferences().theme;
  return theme === "claro" || theme === "escuro" ? theme : "auto";
}
function applyTheme(choice = themeChoice()) {
  const dark = choice === "escuro" || (choice === "auto" && systemDark.matches);
  document.documentElement.dataset.theme = dark ? "dark" : "light";
  document.querySelector('meta[name="theme-color"]')?.setAttribute("content", dark ? "#000000" : "#f6fbff");
  const toggle = document.querySelector("#theme-toggle");
  if (toggle) {
    const label = dark ? "Ativar tema claro" : "Ativar tema escuro";
    toggle.setAttribute("aria-label", label);
    toggle.title = label;
    toggle.innerHTML = icon(dark ? "sun" : "moon");
  }
  const radio = document.querySelector(`input[name="settings-theme"][value="${choice}"]`);
  if (radio) radio.checked = true;
}
function saveTheme(choice) {
  try {
    localStorage.setItem(preferencesKey, JSON.stringify({ ...savedPreferences(), theme: choice }));
  } catch {
    showToast("Não foi possível guardar o tema neste navegador.");
  }
  applyTheme(choice);
}
applyTheme();
systemDark.addEventListener("change", () => { if (themeChoice() === "auto") applyTheme(); });
document.querySelector("#theme-toggle").addEventListener("click", () => {
  saveTheme(document.documentElement.dataset.theme === "dark" ? "claro" : "escuro");
});
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

let locationChoice = null;

function setupLocationSelector() {
  const modal = document.querySelector("#location-modal");
  const citySelect = document.querySelector("#location-city");
  const confirm = document.querySelector("#location-confirm");
  const recentSection = document.querySelector("#location-recent-section");
  const recentContainer = document.querySelector("#location-recent");
  const $ = window.jQuery;
  let $city = null;
  let openRequest = 0;
  const locationId = (city, state) => `${city}::${state}`;

  function close() {
    openRequest += 1;
    modal.hidden = true;
    document.body.classList.remove("location-modal-open");
    document.querySelector("[data-open-location-modal]")?.focus();
  }

  function selectCity(city, state, syncWidget = true) {
    if (!city || !state) return;
    const id = locationId(city, state);
    let option = [...citySelect.options].find(item => item.value === id);
    if (!option) {
      option = new Option(`${city} — ${state}`, id, true, true);
      citySelect.add(option);
    }
    citySelect.value = id;
    locationChoice = { city, state };
    confirm.disabled = false;
    if (syncWidget && $city) $city.val(id).trigger("change.select2");
  }

  function recentLocations() {
    const locations = savedPreferences().recentCities;
    return Array.isArray(locations) ? locations.filter(item => item?.city && item?.state).slice(0, 5) : [];
  }

  function renderRecent() {
    const locations = recentLocations();
    recentContainer.replaceChildren();
    recentSection.hidden = locations.length === 0;
    locations.forEach(item => {
      const button = document.createElement("button");
      button.type = "button";
      button.textContent = `${item.city}, ${item.state}`;
      button.addEventListener("click", () => selectCity(item.city, item.state));
      recentContainer.append(button);
    });
  }

  if ($?.fn?.select2) {
    $city = $(citySelect);
    $city.select2({
      width: "100%",
      dropdownParent: $(modal),
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
          estacaoApi.sugestoes(params.data.term || "").then(success).catch(failure);
          return { abort() {} };
        },
        processResults: suggestions => ({
          results: suggestions.map(item => ({
            id: locationId(item.cidade, item.estado),
            text: `${item.cidade} — ${item.nomeEstado || item.estado}`,
            cidade: item.cidade,
            estado: item.estado
          }))
        })
      }
    });
    $city.on("select2:select", event => selectCity(event.params.data.cidade, event.params.data.estado, false));
  }

  async function open() {
    const request = ++openRequest;
    locationChoice = null;
    confirm.disabled = true;
    renderRecent();
    modal.hidden = false;
    document.body.classList.add("location-modal-open");
    try {
      const station = await estacaoApi.ativa();
      if (request === openRequest && !locationChoice) selectCity(station.cidade, station.estado);
    } catch {
      const saved = savedPreferences();
      if (request === openRequest && !locationChoice) selectCity(saved.city, saved.state);
    }
    if ($city) $city.select2("open");
  }

  document.querySelectorAll("[data-open-location-modal]").forEach(button => button.addEventListener("click", open));
  document.querySelectorAll("[data-close-location-modal]").forEach(button => button.addEventListener("click", close));
  modal.addEventListener("click", event => { if (event.target === modal) close(); });
  document.addEventListener("keydown", event => { if (event.key === "Escape" && !modal.hidden) close(); });
  document.querySelectorAll("[data-location-city]").forEach(button => button.addEventListener("click", () =>
    selectCity(button.dataset.locationCity, button.dataset.locationState)));
  confirm.addEventListener("click", async () => {
    if (!locationChoice) return;
    confirm.disabled = true;
    try {
      const station = await estacaoApi.selecionar(locationChoice.city, locationChoice.state);
      const preferences = savedPreferences();
      const recentCities = [{ city: station.cidade, state: station.estado }, ...recentLocations()
        .filter(item => !(item.city === station.cidade && item.state === station.estado))].slice(0, 5);
      localStorage.setItem(preferencesKey, JSON.stringify({ ...preferences, city: station.cidade, state: station.estado, recentCities }));
      showLocation({ city: station.cidade, state: station.estado });
      close();
      showToast("Cidade atualizada. Os dados começam a ser carregados em instantes.");
      refreshAfterCityChange();
    } catch (error) {
      showToast(error.message || "Não foi possível atualizar a cidade.");
      confirm.disabled = false;
    }
  });
}
setupLocationSelector();

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
let historyRequest = 0;
let pageRefreshInProgress = false;
let homeChartRange = "24h";
let homeChartRequest = 0;

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
    probabilidadeChuva: resumo.probabilidadeChuva,
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

const rainCodes = [51, 53, 55, 61, 63, 65, 80, 81, 82, 95, 96, 99];

// Fundo do card "Condições atuais": chuva vem da leitura; dia/noite vem do horário atual.
function updateHeroScene(reading) {
  const hero = document.querySelector(".weather-hero");
  if (!hero) return;
  const hour = new Date().getHours();
  const night = hour < 6 || hour >= 18;
  const rainy = Boolean(reading) && (reading.presencaPrecipitacao === true || rainCodes.includes(Number(reading.codigoClima)));
  hero.dataset.scene = `${rainy ? "chuva" : "limpo"}-${night ? "noite" : "dia"}`;
}

function renderHomeReading(reading, recent) {
  updateHeroScene(reading);
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
  // O cartão de chuva conserva a última previsão válida enquanto a nova consulta carrega.
  text("#metric-wind", number(reading.velocidadeVento));
  const first = recent.at(0);
  const temperatures = numericValues(recent, "temperatura");
  const humidity = numericValues(recent, "umidadeRelativa");
  text("#temperature-detail", temperatures.length ? `Mín. ${unit(Math.min(...temperatures), "°C")} · Máx. ${unit(Math.max(...temperatures), "°C")}` : "Sem histórico comparável");
  text("#humidity-detail", humidity.length ? `Mín. ${number(Math.min(...humidity), 0)}% · Máx. ${number(Math.max(...humidity), 0)}%` : "Sem histórico comparável");
  text("#wind-detail", "Velocidade na última leitura");
  const delta = first && first.temperatura !== null && reading.temperatura !== null ? Number(reading.temperatura) - Number(first.temperatura) : null;
  text("#temperature-detail-trend", delta === null ? "Sem tendência disponível" : `${delta >= 0 ? "↗ +" : "↘ "}${number(delta)} °C nas últimas leituras`);
  text("#humidity-detail-trend", simulated() ? "Dados simulados" : "Dados da estação");
  text("#wind-detail-trend", simulated() ? "Dados simulados" : "Dados da estação");
}

function iconeDoClimaHorario(item) {
  const chance = Number(item.probabilidadeChuva ?? item.probabilidadeChuvaMaxima);
  if (rainCodes.includes(Number(item.codigoClima)) || Number.isFinite(chance) && chance >= 40) return "rain";
  if ([0, 1, 2].includes(Number(item.codigoClima))) return "sun";
  return "cloud";
}

// Meteocons Fill (MIT): o recorte de Hoje usa a variante animada; os demais são estáticos.
function assetMeteocon(tipo) {
  return {
    sun: "partly-cloudy-day",
    cloud: "overcast-day",
    rain: "rain",
  }[tipo] || "overcast-day";
}

function simboloClimaHorario(tipo) {
  return `<img class="hourly-weather-symbol" src="https://cdn.meteocons.com/3.0.0-next.10/svg/fill/${assetMeteocon(tipo)}.svg" alt="" aria-hidden="true" decoding="async">`;
}

function simboloClimaEstatico(tipo) {
  return `<img class="forecast-meteocon" src="https://cdn.meteocons.com/3.0.0-next.10/svg-static/fill/${assetMeteocon(tipo)}.svg" alt="" aria-hidden="true" decoding="async">`;
}

function graficoDeProbabilidadeChuva(previsoes) {
  const probabilidades = previsoes.map(item => Number(item.probabilidadeChuva)).filter(Number.isFinite);
  if (!probabilidades.length) return "";
  const pontos = previsoes.map((item, index) => {
    const divisor = previsoes.length - 1;
    const x = previsoes.length === 1 ? 50 : index === 0 ? 25 / divisor : index === divisor ? 100 - 25 / divisor : index / divisor * 100;
    // Percentuais usam a escala 0–100%, mas ficam dentro da área visual do cartão.
    const chance = Math.min(100, Math.max(0, Number(item.probabilidadeChuva)));
    const y = Number.isFinite(chance) ? 78 - chance * .32 : 78;
    return { x, y };
  });
  const linha = pontos.map(({ x, y }) => `${x},${y}`).join(" ");
  const area = `M ${pontos[0].x},84 L ${linha.replaceAll(" ", " L ")} L ${pontos.at(-1).x},84 Z`;
  const linhasHorizontais = [46, 57, 68, 79].map(y => `<line class="hourly-grid-line" x1="0" x2="100" y1="${y}" y2="${y}" />`).join("");
  const linhasVerticais = pontos.map(({ x }) => `<line class="hourly-grid-line hourly-grid-line-vertical" x1="${x}" x2="${x}" y1="43" y2="84" />`).join("");
  return `<svg class="hourly-rain-probability-line" viewBox="0 0 100 100" preserveAspectRatio="none" aria-hidden="true"><defs><linearGradient id="hourly-rain-probability-fill" x1="0" x2="0" y1="0" y2="1"><stop offset="0" stop-color="#38a9f8" stop-opacity=".26"/><stop offset="1" stop-color="#38a9f8" stop-opacity=".015"/></linearGradient></defs>${linhasHorizontais}${linhasVerticais}<path class="hourly-rain-probability-area" d="${area}"/><polyline points="${linha}" /></svg><span class="hourly-rain-probability-dots" aria-hidden="true">${pontos.map(({ x, y }) => `<i style="left:${x}%;top:${y}%"></i>`).join("")}</span>`;
}

function horarioEmFormato24h(value) {
  const data = parseDate(value);
  if (!data) return "—";
  return `${String(data.getHours()).padStart(2, "0")}:${String(data.getMinutes()).padStart(2, "0")}`;
}

function atualizarCartaoChuva(previsoes) {
  const proximaPrevisao = previsoes.find(item => Number.isFinite(Number(item.probabilidadeChuva))) || previsoes[0];
  const chance = Number(proximaPrevisao?.probabilidadeChuva);
  const precipitacao = Number(proximaPrevisao?.precipitacao);
  text("#metric-rain-chance", Number.isFinite(chance) ? number(chance, 0) : "—");
  text("#rain-detail", `Precipitação prevista: ${Number.isFinite(precipitacao) ? unit(precipitacao, "mm") : "—"}`);
  text("#rain-detail-trend", proximaPrevisao ? `Previsão para ${horarioEmFormato24h(proximaPrevisao.dataHora)}` : "Previsão horária indisponível");
}

function desenharGraficoClimaHorario(previsoes, fonte) {
  const canvas = document.querySelector("#hourly-weather-rows");
  if (!canvas) return;
  if (!previsoes.length) {
    canvas.innerHTML = '<div class="chart-empty">A previsão por hora ainda não está disponível.</div>';
    return;
  }
  text("#hourly-weather-subtitle", fonte);
  atualizarCartaoChuva(previsoes);
  canvas.setAttribute("aria-label", "Previsão por hora com probabilidade de chuva");
  canvas.innerHTML = `${graficoDeProbabilidadeChuva(previsoes)}<div class="hourly-weather-points">${previsoes.map(item => {
    const hora = horarioEmFormato24h(item.dataHora);
    const chance = Number(item.probabilidadeChuva);
    const tipo = iconeDoClimaHorario(item);
    return `<article class="hourly-weather-point"><time>${hora}</time><span class="hourly-weather-icon ${tipo}">${simboloClimaHorario(tipo)}</span><strong>${number(item.temperatura, 0)}°</strong><span class="hourly-rain-chance">${icon("drop")} ${Number.isFinite(chance) ? `${number(chance, 0)}%` : "—"}</span></article>`;
  }).join("")}</div>`;
}

function previsaoHorariaDeExemplo() {
  const inicio = new Date();
  inicio.setMinutes(0, 0, 0);
  const chances = [10, 15, 20, 35, 50, 60, 45, 30];
  return chances.map((probabilidadeChuva, indice) => ({
    dataHora: new Date(inicio.getTime() + indice * 3_600_000).toISOString(),
    temperatura: 25 - Math.min(indice, 6), probabilidadeChuva,
    precipitacao: probabilidadeChuva >= 40 ? (probabilidadeChuva - 30) / 10 : 0,
    codigoClima: probabilidadeChuva >= 40 ? 61 : indice < 2 ? 1 : 3,
  }));
}

async function renderGraficoClimaHorario() {
  if (simulated()) {
    desenharGraficoClimaHorario(previsaoHorariaDeExemplo(), "Exemplo de previsão por hora");
    return;
  }
  const inicio = new Date();
  inicio.setMinutes(0, 0, 0);
  // O painel foi desenhado para uma faixa legível de oito horas, incluindo a atual.
  const fim = new Date(inicio.getTime() + 7 * 3_600_000);
  try {
    const previsoes = await resumosHorariosApi.buscarPrevisoes(apiDate(inicio), apiDate(fim));
    desenharGraficoClimaHorario(previsoes, "Hoje · previsão por hora da Open-Meteo");
  } catch (error) {
    const canvas = document.querySelector("#hourly-weather-rows");
    if (canvas) canvas.innerHTML = `<div class="chart-empty">${error.message}</div>`;
    text("#hourly-weather-subtitle", "Previsão horária indisponível");
  }
}

async function renderHomeChart(range = homeChartRange) {
  homeChartRange = range;
  setRangeButtons(range, ["home-chart-range"]);
  const hourlyCanvas = document.querySelector("#hourly-weather-rows");
  const dailyPanel = document.querySelector("#home-daily-forecast");
  const chartPanel = document.querySelector(".hourly-weather-chart");
  if (!hourlyCanvas || !dailyPanel) return;
  const hourly = range === "24h";
  chartPanel?.classList.toggle("daily-forecast-mode", !hourly);
  hourlyCanvas.hidden = !hourly;
  dailyPanel.hidden = hourly;
  if (hourly) {
    await renderGraficoClimaHorario();
    return;
  }
  const request = ++homeChartRequest;
  const limite = range === "7d" ? 7 : 15;
  text("#hourly-weather-subtitle", `Próximos ${limite} dias · previsão diária da Open-Meteo`);
  try {
    const previsoes = await buscarPrevisoesDiarias(limite);
    if (request !== homeChartRequest || homeChartRange !== range) return;
    renderPrevisaoDiariaNoPainel(previsoes, limite);
    if (!previsoes.length) text("#hourly-weather-subtitle", `Próximos ${limite} dias · previsão ainda indisponível`);
  } catch (error) {
    if (request !== homeChartRequest || homeChartRange !== range) return;
    document.querySelector("#home-daily-forecast-scroll").innerHTML = `<div class="chart-empty">${error.message}</div>`;
    text("#hourly-weather-subtitle", "Previsão indisponível");
  }
}

function previsoesDiariasDeExemplo(limite) {
  const inicio = new Date();
  inicio.setHours(12, 0, 0, 0);
  return Array.from({ length: limite }, (_, indice) => {
    const data = new Date(inicio);
    data.setDate(data.getDate() + indice);
    const chance = [14, 21, 38, 60, 75, 48, 25, 15][indice % 8];
    return {
      data: dateInput(data), temperaturaMinima: 15 + indice % 3, temperaturaMaxima: 24 - indice % 4,
      precipitacaoTotal: chance >= 40 ? chance / 25 : 0, probabilidadeChuvaMaxima: chance,
      codigoClima: chance >= 40 ? 61 : indice % 3 === 0 ? 1 : 3,
    };
  });
}

async function buscarPrevisoesDiarias(limite) {
  return simulated() ? previsoesDiariasDeExemplo(limite) : resumosDiariosApi.buscarPrevisoes(limite);
}

function dataDaPrevisao(item) {
  return item.data ? parseDate(`${item.data}T12:00:00`) : item.day instanceof Date ? item.day : null;
}

function rotuloDaPrevisao(item, indice = 0) {
  const data = dataDaPrevisao(item);
  if (!data) return "—";
  return indice === 0 ? "Hoje" : new Intl.DateTimeFormat("pt-BR", { weekday: "short" }).format(data).replace(".", "");
}

function detalhesChuvaDaPrevisao(item) {
  const chance = Number(item.probabilidadeChuvaMaxima ?? item.probabilidadeChuva);
  const mm = Number(item.precipitacaoTotal ?? item.precipitacao ?? item.rain);
  return { chance, mm };
}

function renderPrevisaoDiariaNoPainel(previsoes, limite) {
  const scroller = document.querySelector("#home-daily-forecast-scroll");
  if (!scroller) return;
  if (!previsoes.length) {
    scroller.innerHTML = '<div class="chart-empty">A previsão diária ainda não está disponível.</div>';
    return;
  }
  scroller.setAttribute("aria-label", `Previsão para os próximos ${Math.min(limite, previsoes.length)} dias`);
  scroller.innerHTML = previsoes.slice(0, limite).map((item, indice) => {
    const data = dataDaPrevisao(item);
    const { chance, mm } = detalhesChuvaDaPrevisao(item);
    const tipo = iconeDoClimaHorario(item);
    return `<article class="daily-forecast-item"><time>${rotuloDaPrevisao(item, indice)}<small>${data ? new Intl.DateTimeFormat("pt-BR", { day: "2-digit", month: "2-digit" }).format(data) : ""}</small></time>${simboloClimaEstatico(tipo)}<strong>${number(item.temperaturaMaxima, 0)}° <small>${number(item.temperaturaMinima, 0)}°</small></strong><span class="daily-forecast-rain">${icon("drop")}${Number.isFinite(chance) ? `${number(chance, 0)}%` : "—"}</span></article>`;
  }).join("");
}

function renderDemoForecast() {
  if (!simulated()) return;
  text("#forecast-source", "Exemplo de previsão simulada");
  text("#forecast-note", "Previsão ilustrativa gerada neste navegador.");
  text("#overview-data-note", "Modo simulado: todos os valores desta tela são exemplos locais. Nenhum deles é gravado no MySQL.");
  document.querySelector("#home-source-chip").innerHTML = `${icon("flask")} Fonte: Simulado`;
  const rows = document.querySelector("#forecast-rows");
  rows.setAttribute("aria-label", "Previsão simulada para cinco dias");
  rows.innerHTML = previsoesDiariasDeExemplo(5).map(renderLinhaPrevisaoDiaria).join("");
}

function renderLinhaPrevisaoDiaria(item, indice) {
  const data = dataDaPrevisao(item);
  const { chance } = detalhesChuvaDaPrevisao(item);
  const tipo = iconeDoClimaHorario(item);
  const dataCurta = data ? new Intl.DateTimeFormat("pt-BR", { day: "2-digit", month: "2-digit" }).format(data) : "";
  return `<div class="forecast-row"><span><strong>${rotuloDaPrevisao(item, indice)}</strong><small>${dataCurta}</small></span><span class="forecast-weather">${simboloClimaEstatico(tipo)}</span><span class="forecast-rain">${Number.isFinite(chance) ? `${number(chance, 0)}%` : "—"}</span><span class="forecast-temperatures"><strong>${number(item.temperaturaMaxima, 0)}°</strong><small>${number(item.temperaturaMinima, 0)}°</small></span></div>`;
}

async function renderForecast() {
  if (simulated()) {
    renderDemoForecast();
    return;
  }
  const rows = document.querySelector("#forecast-rows");
  try {
    const previsoes = await buscarPrevisoesDiarias(5);
    if (!previsoes.length) throw new Error("A previsão ainda não foi carregada pelo coletor.");
    text("#forecast-source", "Previsão da Open-Meteo");
    text("#forecast-note", "Atualizada pelo coletor; dias futuros não são leituras da estação.");
    rows.setAttribute("aria-label", "Previsão para os próximos cinco dias");
    rows.innerHTML = previsoes.map(renderLinhaPrevisaoDiaria).join("");
  } catch (error) {
    text("#forecast-source", "Previsão ainda indisponível");
    text("#forecast-note", "Inicie o coletor para carregar os próximos dias.");
    rows.innerHTML = `<div class="forecast-row"><span><strong>—</strong><small>sem dados</small></span><span>—</span><span>—</span><span>—</span><span class="forecast-rain">—</span></div>`;
  }
}

async function refreshHome() {
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
  await renderHomeChart(homeChartRange);
}

async function initHome() {
  document.querySelector("#home-chart-range").addEventListener("click", event => {
    const button = event.target.closest("[data-range]");
    if (button) renderHomeChart(button.dataset.range);
  });
  await refreshHome();
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
  setHistoryPreset("24h");
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
  document.querySelector("#settings-interval").value = saved.interval || "900";
  document.querySelector("#settings-notes").value = saved.notes || "";
  document.querySelector(`input[name="settings-mode"][value="${saved.mode === "simulado" ? "simulado" : "real"}"]`).checked = true;
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
  applyTheme();
  document.querySelectorAll('input[name="settings-theme"]').forEach(radio => {
    radio.addEventListener("change", () => { if (radio.checked) saveTheme(radio.value); });
  });
  document.querySelector("#save-settings").addEventListener("click", async () => {
    const preferences = {
      interval: document.querySelector("#settings-interval").value,
      mode: document.querySelector('input[name="settings-mode"]:checked').value,
      notes: document.querySelector("#settings-notes").value.trim(),
    };
    try {
      localStorage.setItem(preferencesKey, JSON.stringify({ ...savedPreferences(), ...preferences }));
      applyMode(preferences.mode);
      showToast("Configurações salvas neste navegador.");
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

async function refreshCurrentPage() {
  if (document.hidden || pageRefreshInProgress) return;
  pageRefreshInProgress = true;
  try {
    if (path === "/") {
      await refreshHome();
    } else if (path === "/historico") {
      await fetchLatest();
      if (historyStart && historyEnd) await loadHistory(historyStart, historyEnd);
    } else if (path === "/configuracoes") {
      await fetchLatest();
      text("#settings-last-reading", latestReading ? dateTime(latestReading.dataHoraColeta) : "—");
      text("#settings-last-badge", latestReading ? "Agora" : "Indisponível");
    } else {
      await fetchLatest();
    }
  } finally {
    pageRefreshInProgress = false;
  }
}

function refreshAfterCityChange() {
  // A primeira tentativa é imediata; as demais acompanham a coleta e a sincronização da nova cidade.
  [0, 2_000, 5_000, 10_000].forEach(delay => window.setTimeout(() => { void refreshCurrentPage(); }, delay));
}

function startBackgroundRefresh() {
  window.setInterval(() => { void refreshCurrentPage(); }, 10_000);
  window.addEventListener("focus", () => { void refreshCurrentPage(); });
  document.addEventListener("visibilitychange", () => {
    if (!document.hidden) void refreshCurrentPage();
  });
}

({ "/": initHome, "/historico": initHistory, "/simulacoes": initSimulations, "/configuracoes": initSettings })[path]();
startBackgroundRefresh();
