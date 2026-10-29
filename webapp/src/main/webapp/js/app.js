import { leiturasApi, resumosDiariosApi, resumosHorariosApi, estacaoApi } from "./api.js?v=20261028-6";
import { demoApi } from "./demo-data.js?v=20261026-4";
import { drawTemporalChart, clearTemporalChart } from "./temporal-charts.js?v=20261029-8";
import { overviewPage, historyPage, settingsPage } from "./pages.js?v=20261029-21";
import { anchorDate, apiDate, dateInput, dateTime, fillTable, icon, number, parseDate, renderShell, showToast, text, time, unit, updateCollectorCard } from "./ui.js?v=20261029-13";

const routeFromLocation = () => ({ "/historico": "/historico", "/configuracoes": "/configuracoes" })[window.location.pathname.replace(/\/$/, "")] || "/";
let path = routeFromLocation();
const pages = { "/": overviewPage, "/historico": historyPage, "/configuracoes": settingsPage };
const pageHeadings = {
  "/": ["Visão geral", "Acompanhe em tempo real as condições meteorológicas"],
  "/historico": ["Histórico e análises", "Visualize a evolução das variáveis meteorológicas"],
  "/configuracoes": ["Configurações", "Ajuste os parâmetros de coleta de dados e o comportamento do sistema."],
};
renderShell(path, pages[path]());
function showInitialLoadingSkeleton() {
  document.querySelector("#app").insertAdjacentHTML("beforeend", `
    <div class="initial-page-skeleton" role="status" aria-live="polite" aria-label="Carregando dados">
      <aside class="skeleton-sidebar"><i></i><i></i><i></i><i></i><i></i></aside>
      <main class="skeleton-workspace"><div class="skeleton-heading"><i></i><i></i></div><div class="skeleton-hero"></div><div class="skeleton-metrics"><i></i><i></i><i></i><i></i></div><div class="skeleton-chart"></div><span>Carregando dados…</span></main>
    </div>`);
  document.querySelector("#app").setAttribute("aria-busy", "true");
}
function hideInitialLoadingSkeleton() {
  document.querySelector(".initial-page-skeleton")?.remove();
  document.querySelector("#app").removeAttribute("aria-busy");
}
showInitialLoadingSkeleton();
const preferencesKey = "meteora-ui-preferences";
function savedPreferences() {
  try {
    const value = JSON.parse(localStorage.getItem(preferencesKey) || "{}");
    return value && typeof value === "object" ? value : {};
  }
  catch { return {}; }
}
function applySidebarState(collapsed) {
  document.querySelector(".app-layout")?.classList.toggle("sidebar-collapsed", collapsed);
  const toggle = document.querySelector("#sidebar-collapse-toggle");
  if (toggle) {
    const label = collapsed ? "Expandir menu" : "Recolher menu";
    toggle.setAttribute("aria-label", label);
    toggle.title = label;
  }
}
function setupSidebarCollapse() {
  applySidebarState(Boolean(savedPreferences().sidebarCollapsed));
  document.querySelector("#sidebar-collapse-toggle")?.addEventListener("click", () => {
    const collapsed = !document.querySelector(".app-layout")?.classList.contains("sidebar-collapsed");
    try {
      localStorage.setItem(preferencesKey, JSON.stringify({ ...savedPreferences(), sidebarCollapsed: collapsed }));
    } catch {
      // A preferência é apenas visual; a navegação segue funcionando sem armazenamento local.
    }
    applySidebarState(collapsed);
  });
}
setupSidebarCollapse();
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
  const label = city ? `${city}${state === "São Paulo" ? ", SP" : state ? `, ${state}` : ""}` : "Localização da estação";
  document.querySelectorAll("[data-location-label]").forEach(element => { element.textContent = label; });
}
showLocation(savedPreferences());

async function showConfiguredLocation() {
  try {
    const source = await leiturasApi.buscarFonte();
    if (source.cidade) document.querySelectorAll("[data-location-label]").forEach(element => { element.textContent = `${source.cidade}${source.estado ? `, ${source.estado}` : ""}`; });
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
      localStorage.setItem(preferencesKey, JSON.stringify({ ...preferences, city: station.cidade, state: station.estado }));
      showLocation({ city: station.cidade, state: station.estado });
      delete document.querySelector("#hourly-weather-rows")?.dataset.hourlyFocusedDate;
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
let historyForecasts = [];
let historicalDailyWeather = [];
let historyStart = null;
let historyEnd = null;
let historyRequest = 0;
let historyCurrentPage = 1;
const HISTORY_PAGE_SIZE = 10;
let pageRefreshInProgress = false;
let homeChartRange = "24h";
let homeChartRequest = 0;
let recentReadings = [];
let recentCurrentPage = 1;
const RECENT_PAGE_SIZE = 15;
// A coleta permanece em 10 s conforme o enunciado; a interface não precisa
// repetir todas as consultas nessa mesma frequência.
const UI_REFRESH_INTERVAL_MS = 30_000;

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

function previsaoNoturna(item) {
  const horario = item.dataHora || item.dataHoraColeta || (item.data ? `${item.data}T12:00:00` : null);
  const data = parseDate(horario);
  return data ? data.getHours() < 6 || data.getHours() >= 18 : false;
}

// Códigos WMO da Open-Meteo. A visualização horária usa SVGs animados da
// Meteocons; a diária mantém os mesmos símbolos em SVG estático.
function iconeDoClimaHorario(item) {
  const codigo = Number(item.codigoClima);
  const noturno = previsaoNoturna(item);
  if (codigo === 0) return noturno ? "clear-night" : "clear-day";
  if ([1, 2].includes(codigo)) return noturno ? "partly-cloudy-night" : "partly-cloudy-day";
  if (codigo === 3) return noturno ? "overcast-night" : "overcast-day";
  if ([45, 48].includes(codigo)) return noturno ? "fog-night" : "fog-day";
  if ([51, 53, 55].includes(codigo)) return "drizzle";
  if ([56, 57, 66, 67].includes(codigo)) return "sleet";
  if ([61, 63, 65, 80, 81, 82].includes(codigo)) return "rain";
  if ([71, 73, 75, 77, 85, 86].includes(codigo)) return "snow";
  if ([95, 96, 99].includes(codigo)) return "thunderstorms-rain";
  const chance = Number(item.probabilidadeChuva ?? item.probabilidadeChuvaMaxima);
  return Number.isFinite(chance) && chance >= 40 ? "rain" : noturno ? "partly-cloudy-night" : "partly-cloudy-day";
}

function simboloClimaHorario(asset) {
  return `<img class="hourly-weather-symbol" src="https://cdn.meteocons.com/3.0.0-next.10/svg/fill/${asset}.svg" alt="" aria-hidden="true" decoding="async">`;
}

function simboloClimaEstatico(asset) {
  return `<img class="forecast-meteocon" src="https://cdn.meteocons.com/3.0.0-next.10/svg-static/fill/${asset}.svg" alt="" aria-hidden="true" decoding="async">`;
}

function iconeResumo(asset) {
  return `<span class="summary-meteocon"><img src="https://cdn.meteocons.com/3.0.0-next.10/svg-static/fill/${asset}.svg" alt="" aria-hidden="true" decoding="async"></span>`;
}

function graficoDeTemperaturaHoraria(previsoes, largura) {
  const temperaturas = previsoes.map(item => Number(item.temperatura)).filter(Number.isFinite);
  if (!temperaturas.length) return "";
  const minima = Math.min(...temperaturas);
  const maxima = Math.max(...temperaturas);
  const media = temperaturas.reduce((soma, temperatura) => soma + temperatura, 0) / temperaturas.length;
  const semiAmplitude = Math.max((maxima - minima) / 2, .5);
  const pontos = previsoes.map((item, index) => {
    const divisor = previsoes.length - 1;
    const x = previsoes.length === 1 ? 50 : index === 0 ? 25 / divisor : index === divisor ? 100 - 25 / divisor : index / divisor * 100;
    // A curva traduz somente a variação térmica. A média fica no centro da
    // área gráfica; mínimo e máximo usam a amplitude disponível sem alcançar
    // os rótulos de temperatura, acima, nem a chance de chuva, abaixo.
    const temperatura = Number(item.temperatura);
    const desvioNormalizado = Number.isFinite(temperatura)
      ? Math.max(-1, Math.min(1, (temperatura - media) / semiAmplitude)) : 0;
    const y = 53 - desvioNormalizado * 11;
    return { x, y };
  });
  const linha = pontos.map(({ x, y }) => `${x},${y}`).join(" ");
  const area = `M ${pontos[0].x},70 L ${linha.replaceAll(" ", " L ")} L ${pontos.at(-1).x},70 Z`;
  const linhasHorizontais = [42, 49, 56, 63].map(y => `<line class="hourly-grid-line" x1="0" x2="100" y1="${y}" y2="${y}" />`).join("");
  const linhasVerticais = pontos.map(({ x }) => `<line class="hourly-grid-line hourly-grid-line-vertical" x1="${x}" x2="${x}" y1="40" y2="70" />`).join("");
  return `<svg class="hourly-temperature-line" style="width:${largura}px" viewBox="0 0 100 100" preserveAspectRatio="none" aria-hidden="true"><defs><linearGradient id="hourly-temperature-fill" x1="0" x2="0" y1="0" y2="1"><stop offset="0" stop-color="#38a9f8" stop-opacity=".26"/><stop offset="1" stop-color="#38a9f8" stop-opacity=".015"/></linearGradient></defs>${linhasHorizontais}${linhasVerticais}<path class="hourly-temperature-area" d="${area}"/><polyline points="${linha}" /></svg><span class="hourly-temperature-dots" style="width:${largura}px" aria-hidden="true">${pontos.map(({ x, y }) => `<i style="left:${x}%;top:${y}%"></i>`).join("")}</span>`;
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

function desenharGraficoClimaHorario(previsoes) {
  const canvas = document.querySelector("#hourly-weather-rows");
  if (!canvas) return;
  if (!previsoes.length) {
    canvas.innerHTML = '<div class="chart-empty">A previsão por hora ainda não está disponível.</div>';
    return;
  }
  atualizarCartaoChuva(previsoes);
  canvas.setAttribute("aria-label", "Previsão por hora com linha de temperatura e probabilidade de chuva");
  // Mantém cada hora legível e permite navegar pelo restante do dia sem comprimir ícones ou pontos.
  const largura = Math.max(canvas.clientWidth, previsoes.length * 104);
  canvas.innerHTML = `${graficoDeTemperaturaHoraria(previsoes, largura)}<div class="hourly-weather-points" style="--hourly-items:${previsoes.length};width:${largura}px">${previsoes.map(item => {
    const hora = horarioEmFormato24h(item.dataHora);
    const chance = Number(item.probabilidadeChuva);
    const asset = iconeDoClimaHorario(item);
    return `<article class="hourly-weather-point"><time>${hora}</time><span class="hourly-weather-icon ${asset}">${simboloClimaHorario(asset)}</span><strong>${number(item.temperatura, 0)}°</strong><span class="hourly-rain-chance">${icon("drop")} ${Number.isFinite(chance) ? `${number(chance, 0)}%` : "—"}</span></article>`;
  }).join("")}</div>`;
  const hoje = dateInput(new Date());
  if (canvas.dataset.hourlyFocusedDate !== hoje) {
    const horaAtual = new Date().getHours();
    const indiceAtual = previsoes.findIndex(item => parseDate(item.dataHora)?.getHours() === horaAtual);
    if (indiceAtual >= 0) {
      requestAnimationFrame(() => {
        const passo = largura / previsoes.length;
        canvas.scrollLeft = Math.max(0, indiceAtual * passo - (canvas.clientWidth - passo) / 2);
      });
    }
    canvas.dataset.hourlyFocusedDate = hoje;
  }
}

function previsaoHorariaDeExemplo() {
  const inicio = new Date();
  inicio.setHours(0, 0, 0, 0);
  const chances = [10, 15, 20, 35, 50, 60, 45, 30];
  return Array.from({ length: 24 }, (_, indice) => ({
    probabilidadeChuva: chances[indice % chances.length],
    dataHora: new Date(inicio.getTime() + indice * 3_600_000).toISOString(),
    temperatura: 25 - Math.min(indice, 6),
    precipitacao: chances[indice % chances.length] >= 40 ? (chances[indice % chances.length] - 30) / 10 : 0,
    codigoClima: chances[indice % chances.length] >= 40 ? 61 : indice < 2 ? 1 : 3,
  }));
}

async function renderGraficoClimaHorario() {
  if (simulated()) {
    desenharGraficoClimaHorario(previsaoHorariaDeExemplo());
    return;
  }
  const inicio = new Date();
  inicio.setHours(0, 0, 0, 0);
  // "Hoje" termina às 23:00: a previsão não é limitada às oito primeiras horas.
  const fim = new Date(inicio);
  fim.setHours(23, 0, 0, 0);
  try {
    const previsoes = await resumosHorariosApi.buscarPrevisoes(apiDate(inicio), apiDate(fim));
    desenharGraficoClimaHorario(previsoes);
  } catch (error) {
    const canvas = document.querySelector("#hourly-weather-rows");
    if (canvas) canvas.innerHTML = `<div class="chart-empty">${error.message}</div>`;
  }
}

const titulosPrevisaoDoTempo = {
  "24h": "Previsão do tempo · Hoje",
  "7d": "Previsão do tempo · Em 7 dias",
  "15d": "Previsão do tempo · Em 15 dias",
};

async function renderHomeChart(range = homeChartRange, resetarNavegacao = false) {
  homeChartRange = range;
  setRangeButtons(range, ["home-chart-range"]);
  text("#hourly-weather-title", titulosPrevisaoDoTempo[range] || titulosPrevisaoDoTempo["24h"]);
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
  try {
    const previsoes = await buscarPrevisoesDiarias(limite);
    if (request !== homeChartRequest || homeChartRange !== range) return;
    renderPrevisaoDiariaNoPainel(previsoes, limite, resetarNavegacao);
  } catch (error) {
    if (request !== homeChartRequest || homeChartRange !== range) return;
    const scroller = document.querySelector("#home-daily-forecast-scroll");
    scroller.dataset.itemCount = "0";
    scroller.dataset.allowNavigation = "false";
    scroller.classList.remove("fit-seven-days");
    scroller.innerHTML = `<div class="chart-empty">${error.message}</div>`;
    atualizarNavegacaoPrevisaoDiaria();
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

function nomeDoDia(data) {
  if (!data) return "—";
  const day = new Intl.DateTimeFormat("pt-BR", { weekday: "long" }).format(data);
  return `${day.charAt(0).toLocaleUpperCase("pt-BR")}${day.slice(1)}`;
}

function rotuloDaPrevisao(item, indice = 0) {
  const data = dataDaPrevisao(item);
  if (!data) return "—";
  return indice === 0 ? "Hoje" : nomeDoDia(data);
}

function detalhesChuvaDaPrevisao(item) {
  const chance = Number(item.probabilidadeChuvaMaxima ?? item.probabilidadeChuva);
  const mm = Number(item.precipitacaoTotal ?? item.precipitacao ?? item.rain);
  return { chance, mm };
}

function renderPrevisaoDiariaNoPainel(previsoes, limite, resetarNavegacao = false) {
  const scroller = document.querySelector("#home-daily-forecast-scroll");
  if (!scroller) return;
  if (!previsoes.length) {
    scroller.dataset.itemCount = "0";
    scroller.dataset.allowNavigation = "false";
    scroller.classList.remove("fit-seven-days");
    scroller.innerHTML = '<div class="chart-empty">A previsão diária ainda não está disponível.</div>';
    atualizarNavegacaoPrevisaoDiaria();
    return;
  }
  scroller.setAttribute("aria-label", `Previsão para os próximos ${Math.min(limite, previsoes.length)} dias`);
  scroller.dataset.itemCount = String(Math.min(limite, previsoes.length));
  const posicaoAnterior = resetarNavegacao ? 0 : scroller.scrollLeft;
  scroller.innerHTML = previsoes.slice(0, limite).map((item, indice) => {
    const data = dataDaPrevisao(item);
    const { chance, mm } = detalhesChuvaDaPrevisao(item);
    const asset = iconeDoClimaHorario(item);
    return `<article class="daily-forecast-item"><time>${rotuloDaPrevisao(item, indice)}<small>${data ? new Intl.DateTimeFormat("pt-BR", { day: "2-digit", month: "2-digit" }).format(data) : ""}</small></time>${simboloClimaEstatico(asset)}<strong>${number(item.temperaturaMaxima, 0)}° <small>${number(item.temperaturaMinima, 0)}°</small></strong><span class="daily-forecast-rain">${icon("drop")}${Number.isFinite(chance) ? `${number(chance, 0)}%` : "—"}</span></article>`;
  }).join("");
  requestAnimationFrame(() => {
    atualizarNavegacaoPrevisaoDiaria();
    scroller.scrollLeft = posicaoAnterior;
    requestAnimationFrame(atualizarNavegacaoPrevisaoDiaria);
  });
}

function atualizarNavegacaoPrevisaoDiaria() {
  const scroller = document.querySelector("#home-daily-forecast-scroll");
  const anterior = document.querySelector("#daily-forecast-previous");
  const proximo = document.querySelector("#daily-forecast-next");
  if (!scroller) return;
  const quantidade = Number(scroller.dataset.itemCount || scroller.children.length);
  // Sete dias ficam fixos no layout desktop. Em tela estreita, a faixa volta a
  // ser navegável para não comprimir as colunas.
  const semanaCabeNoPainel = quantidade <= 7 && !window.matchMedia("(max-width: 760px)").matches;
  scroller.classList.toggle("fit-seven-days", semanaCabeNoPainel);
  scroller.dataset.allowNavigation = String(quantidade > 0 && !semanaCabeNoPainel);
  // A rolagem personalizada é o único controle de navegação; não há setas laterais.
  if (anterior) anterior.hidden = true;
  if (proximo) proximo.hidden = true;
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
  const asset = iconeDoClimaHorario(item);
  const dataCurta = data ? new Intl.DateTimeFormat("pt-BR", { day: "2-digit", month: "2-digit" }).format(data) : "";
  return `<div class="forecast-row"><span><strong>${rotuloDaPrevisao(item, indice)}</strong><small>${dataCurta}</small></span><span class="forecast-weather">${simboloClimaEstatico(asset)}</span><span class="forecast-rain">${Number.isFinite(chance) ? `${number(chance, 0)}%` : "—"}</span><span class="forecast-temperatures"><strong>${number(item.temperaturaMaxima, 0)}°</strong><small>${number(item.temperaturaMinima, 0)}°</small></span></div>`;
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
    rows.innerHTML = `<div class="forecast-row"><span><strong>—</strong><small>sem dados</small></span><span class="forecast-weather">—</span><span class="forecast-rain">—</span><span class="forecast-temperatures"><strong>—°</strong><small>—°</small></span></div>`;
  }
}

async function refreshHome() {
  await renderForecast();
  await fetchLatest();
  try {
    const inicioDoDia = new Date();
    inicioDoDia.setHours(0, 0, 0, 0);
    const fimDoDia = new Date(inicioDoDia);
    fimDoDia.setHours(23, 59, 59, 999);
    const [leiturasRecentes, leiturasDoDia, previsoesDoDia] = await Promise.all([
      activeReadingsApi.buscarUltimas(100),
      buscarSerieDoPeriodo(inicioDoDia, fimDoDia).catch(() => []),
      simulated()
        ? Promise.resolve(previsaoHorariaDeExemplo().map(resumoHorarioComoLeitura))
        : resumosHorariosApi.buscarPrevisoes(apiDate(inicioDoDia), apiDate(fimDoDia))
          .then(itens => itens.map(resumoHorarioComoLeitura))
          .catch(() => []),
    ]);
    recentReadings = leiturasRecentes;
    const lastValid = [...recentReadings].reverse().find(reading => reading.temperatura != null || reading.umidadeRelativa != null || reading.precipitacao != null || reading.velocidadeVento != null);
    renderHomeReading(lastValid || latestReading, recentReadings);
    renderRecentReadings();
    renderHomeInsights(leiturasDoDia.length ? leiturasDoDia : recentReadings, previsoesDoDia);
  } catch (error) {
    recentReadings = [];
    renderHomeReading(latestReading, []);
    renderRecentReadings(error.message);
    renderHomeInsights();
  }
  await renderHomeChart(homeChartRange);
}

function renderHomeInsights(readings = recentReadings, forecasts = []) {
  const start = new Date();
  start.setHours(0, 0, 0, 0);
  const end = new Date(start);
  end.setHours(23, 59, 59, 999);
  const domain = { start, end, daily: false };
  const charts = [
    ["#home-humidity-chart", "#home-humidity-subtitle", "umidadeRelativa", "%", "#269ceb", "Umidade relativa do ar", "#home-humidity-series-legend"],
    ["#home-rain-chart", "#home-rain-subtitle", "precipitacao", "mm", "#6a95ed", "Chuva registrada hoje", "#home-rain-series-legend"],
    ["#home-wind-chart", "#home-wind-subtitle", "velocidadeVento", "km/h", "#18b979", "Vento registrado hoje", "#home-wind-series-legend"],
  ];
  charts.forEach(([selector, subtitle, field, suffix, color, description, legendSelector]) => {
    const summary = drawTemporalChart(selector, readings, field, suffix, color, domain, forecasts);
    const period = summary?.available ? `${summary.available} horas medidas hoje` : "Sem leituras medidas hoje";
    text(subtitle, `${description} · ${period}`);
    const legend = document.querySelector(legendSelector);
    if (legend) legend.hidden = !summary?.forecast;
  });
}

function renderRecentReadings(errorMessage = null) {
  const totalPages = Math.max(1, Math.ceil(recentReadings.length / RECENT_PAGE_SIZE));
  recentCurrentPage = Math.min(recentCurrentPage, totalPages);
  const start = (recentCurrentPage - 1) * RECENT_PAGE_SIZE;
  const visible = [...recentReadings].reverse().slice(start, start + RECENT_PAGE_SIZE);
  fillTable("#recent-rows", visible, [
      { value: reading => time(reading.dataHoraColeta) },
      { value: reading => unit(reading.temperatura, "°C") },
      { value: reading => unit(reading.umidadeRelativa, "%", 0) },
      { value: reading => unit(reading.precipitacao, "mm") },
      { value: reading => unit(reading.velocidadeVento, "km/h") },
    ], errorMessage || undefined);
  const pagination = document.querySelector("#recent-pagination");
  const pageLabel = document.querySelector("#recent-page-label");
  if (pagination) {
    pagination.hidden = recentReadings.length <= RECENT_PAGE_SIZE;
    pagination.querySelector('[data-recent-page="previous"]').disabled = recentCurrentPage === 1;
    pagination.querySelector('[data-recent-page="next"]').disabled = recentCurrentPage === totalPages;
  }
  if (pageLabel) pageLabel.textContent = `Página ${recentCurrentPage} de ${totalPages}`;
}

async function initHome() {
  document.querySelector("#home-chart-range").addEventListener("click", event => {
    const button = event.target.closest("[data-range]");
    if (button) renderHomeChart(button.dataset.range, true);
  });
  document.querySelector("#recent-pagination").addEventListener("click", event => {
    const direction = event.target.closest("[data-recent-page]")?.dataset.recentPage;
    if (!direction) return;
    recentCurrentPage += direction === "next" ? 1 : -1;
    renderRecentReadings();
  });
  const previsaoDiaria = document.querySelector("#home-daily-forecast-scroll");
  const moverPrevisao = direcao => {
    if (!previsaoDiaria) return;
    previsaoDiaria.scrollBy({ left: direcao * Math.max(previsaoDiaria.clientWidth * .82, 420), behavior: "smooth" });
  };
  document.querySelector("#daily-forecast-previous")?.addEventListener("click", () => moverPrevisao(-1));
  document.querySelector("#daily-forecast-next")?.addEventListener("click", () => moverPrevisao(1));
  previsaoDiaria?.addEventListener("scroll", atualizarNavegacaoPrevisaoDiaria, { passive: true });
  window.addEventListener("resize", atualizarNavegacaoPrevisaoDiaria);
  await refreshHome();
}

function renderSummary(readings) {
  const values = numericValues(readings, "temperatura");
  const rows = document.querySelector("#summary-rows");
  if (!values.length) {
    rows.innerHTML = '<p class="muted">Não há temperaturas válidas neste período.</p>';
    return;
  }
  const average = list => list.length ? list.reduce((sum, value) => sum + value, 0) / list.length : null;
  const humidity = numericValues(readings, "umidadeRelativa");
  const precipitation = numericValues(readings, "precipitacao");
  const wind = numericValues(readings, "velocidadeVento");
  rows.innerHTML = `
    <div class="summary-row">${iconeResumo("thermometer")}<span>Temperatura média</span><strong>${unit(average(values), "°C")}</strong></div>
    <div class="summary-row">${iconeResumo("humidity")}<span>Umidade média</span><strong>${unit(average(humidity), "%", 0)}</strong></div>
    <div class="summary-row">${iconeResumo("rain")}<span>Precipitação total</span><strong>${unit(precipitation.length ? precipitation.reduce((sum, value) => sum + value, 0) : null, "mm")}</strong></div>
    <div class="summary-row">${iconeResumo("wind")}<span>Velocidade média do vento</span><strong>${unit(average(wind), "km/h")}</strong></div>`;
}

function renderHistoricalWeather() {
  const container = document.querySelector("#historical-weather-list");
  if (!container) return;
  if (!historicalDailyWeather.length) {
    container.innerHTML = '<div class="chart-empty">Ainda não há dias medidos para exibir.</div>';
    return;
  }
  container.innerHTML = historicalDailyWeather.slice(-5).reverse().map(item => {
    const data = dataDaPrevisao(item);
    const label = nomeDoDia(data);
    const dataCurta = data ? new Intl.DateTimeFormat("pt-BR", { day: "2-digit", month: "2-digit" }).format(data) : "";
    const chuva = Number(item.precipitacaoTotal);
    const asset = iconeDoClimaHorario(item);
    return `<div class="forecast-row historical-weather-row"><span><strong>${label}</strong><small>${dataCurta}</small></span><span class="forecast-weather">${simboloClimaEstatico(asset)}</span><span class="forecast-rain">${Number.isFinite(chuva) ? unit(chuva, "mm") : "—"}</span><span class="forecast-temperatures"><strong>${number(item.temperaturaMaxima, 0)}°</strong><small>${number(item.temperaturaMinima, 0)}°</small></span></div>`;
  }).join("");
}

async function buscarClimaDosUltimosDias(end) {
  if (simulated()) return [];
  const hoje = new Date();
  hoje.setHours(0, 0, 0, 0);
  const ultimoDiaCompleto = new Date(Math.min(end.getTime(), hoje.getTime() - 1));
  ultimoDiaCompleto.setHours(0, 0, 0, 0);
  const inicio = new Date(ultimoDiaCompleto);
  inicio.setDate(inicio.getDate() - 4);
  if (ultimoDiaCompleto < inicio) return [];
  return resumosDiariosApi.buscarHistorico(dateInput(inicio), dateInput(ultimoDiaCompleto));
}

function redrawHistoryCharts() {
  const variable = document.querySelector("#history-variable").value;
  const settings = variables[variable];
  text("#history-chart-title", `Histórico de ${settings.title}`);
  const domain = { start: historyStart, end: historyEnd, daily: isDailyPeriod(historyStart, historyEnd) };
  const periodSubtitle = (_summary, field) => ({
    temperatura: "Variação da temperatura",
    umidadeRelativa: "Umidade relativa do ar",
    precipitacao: "Chuva registrada no período",
    velocidadeVento: "Velocidade do vento no período",
  })[field] || "Condições registradas no período";
  const summary = drawTemporalChart("#history-chart-canvas", historyReadings, variable, settings.suffix, settings.color, domain, historyForecasts);
  text("#history-chart-subtitle", periodSubtitle(summary, variable));
  const legend = document.querySelector("#history-series-legend");
  if (legend) legend.hidden = summary.forecast === 0;
  const miniCharts = [
    ["#humidity-chart", "umidadeRelativa", "%", "#269ceb", "#humidity-series-legend"],
    ["#rain-chart", "precipitacao", "mm", "#6a95ed", "#rain-series-legend"],
    ["#wind-chart", "velocidadeVento", "km/h", "#18b979", "#wind-series-legend"],
  ];
  miniCharts.forEach(([selector, field, suffix, color, legendSelector], index) => {
    const miniSummary = drawTemporalChart(selector, historyReadings, field, suffix, color, domain, historyForecasts);
    document.querySelectorAll(".mini-chart-card .title-with-icon small")[index].textContent = periodSubtitle(miniSummary, field);
    const miniLegend = document.querySelector(legendSelector);
    if (miniLegend) miniLegend.hidden = miniSummary.forecast === 0;
  });
}

function renderHistoryDetails() {
  const dateFormatter = new Intl.DateTimeFormat("pt-BR", { day: "2-digit", month: "2-digit", year: "numeric" });
  text("#summary-period", `Medido de ${dateFormatter.format(historyStart)} até ${dateFormatter.format(historyEnd)}`);
  renderSummary(historyReadings);
  renderHistoricalWeather();
  const ordered = [...historyReadings].reverse();
  const totalPages = Math.max(1, Math.ceil(ordered.length / HISTORY_PAGE_SIZE));
  historyCurrentPage = Math.min(historyCurrentPage, totalPages);
  const pageStart = (historyCurrentPage - 1) * HISTORY_PAGE_SIZE;
  const visible = ordered.slice(pageStart, pageStart + HISTORY_PAGE_SIZE);
  fillTable("#history-rows", visible, [
    { value: reading => reading.diario ? readingMoment(reading) : dateTime(reading.dataHoraColeta) },
    { value: reading => unit(reading.temperatura, "°C") },
    { value: reading => unit(reading.umidadeRelativa, "%", 0) },
    { value: reading => unit(reading.precipitacao, "mm") },
    { value: reading => unit(reading.velocidadeVento, "km/h") },
    { value: reading => reading.fonteDados || "—", badge: true },
  ]);
  const pagination = document.querySelector("#history-pagination");
  const status = document.querySelector("#history-page-status");
  const pageLabel = document.querySelector("#history-page-label");
  if (status) status.textContent = ordered.length ? `${pageStart + 1}–${Math.min(pageStart + HISTORY_PAGE_SIZE, ordered.length)} de ${ordered.length} registros` : "0 registros";
  if (pagination) {
    pagination.hidden = ordered.length <= HISTORY_PAGE_SIZE;
    pagination.querySelector('[data-history-page="previous"]').disabled = historyCurrentPage === 1;
    pagination.querySelector('[data-history-page="next"]').disabled = historyCurrentPage === totalPages;
  }
  if (pageLabel) pageLabel.textContent = `Página ${historyCurrentPage} de ${totalPages}`;
  redrawHistoryCharts();
}

async function loadHistory(start, end) {
  const request = ++historyRequest;
  historyStart = start;
  historyEnd = end;
  try {
    const hoje = dateInput(new Date());
    const incluirPrevisaoHoraria = !isDailyPeriod(start, end) && dateInput(start) === hoje && dateInput(end) === hoje;
    const [readings, previsoes, climaDosUltimosDias] = await Promise.all([
      buscarSerieDoPeriodo(start, end),
      incluirPrevisaoHoraria
        ? resumosHorariosApi.buscarPrevisoes(apiDate(start), apiDate(end))
            .then(itens => itens.map(resumoHorarioComoLeitura))
            .catch(() => [])
        : Promise.resolve([]),
      buscarClimaDosUltimosDias(end).catch(() => []),
    ]);
    if (request !== historyRequest) return;
    historyReadings = readingsForPeriod(readings, start, end);
    historyForecasts = previsoes;
    historicalDailyWeather = climaDosUltimosDias;
    renderHistoryDetails();
    if (historyReadings.length > 100) showToast("Mostrando os 100 registros mais recentes na tabela.");
  } catch (error) {
    if (request !== historyRequest) return;
    historyReadings = [];
    historyForecasts = [];
    historicalDailyWeather = [];
    renderHistoryDetails();
    showToast(error.message);
  }
}

function setHistoryPreset(range) {
  setRangeButtons(range, ["history-range"]);
  const [start, end] = rangeDates(range);
  historyCurrentPage = 1;
  return loadHistory(start, end);
}

async function initHistory() {
  await fetchLatest();
  await setHistoryPreset("24h");
  document.querySelector("#history-range").addEventListener("click", event => {
    const button = event.target.closest("[data-range]");
    if (button) setHistoryPreset(button.dataset.range);
  });
  document.querySelector("#history-variable").addEventListener("change", redrawHistoryCharts);
  document.querySelector("#history-pagination").addEventListener("click", event => {
    const direction = event.target.closest("[data-history-page]")?.dataset.historyPage;
    if (!direction) return;
    historyCurrentPage += direction === "next" ? 1 : -1;
    renderHistoryDetails();
  });
}

async function initSettings() {
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
  // Atualiza somente os dados da tela aberta. Não recria o shell, nem repete
  // renderizações em sequência após a troca de cidade; o ciclo normal cuida
  // das leituras posteriores do coletor.
  void refreshCurrentPage();
}

function startBackgroundRefresh() {
  window.setInterval(() => { void refreshCurrentPage(); }, UI_REFRESH_INTERVAL_MS);
  window.addEventListener("focus", () => { void refreshCurrentPage(); });
  document.addEventListener("visibilitychange", () => {
    if (!document.hidden) void refreshCurrentPage();
  });
}

async function initializeCurrentPage() {
  try {
    await ({ "/": initHome, "/historico": initHistory, "/configuracoes": initSettings })[path]();
  } catch (error) {
    showToast(error.message || "Não foi possível carregar os dados iniciais.");
  } finally {
    hideInitialLoadingSkeleton();
  }
}

function atualizarNavegacaoAtiva() {
  const [title, subtitle] = pageHeadings[path];
  document.title = `${title} · Meteora`;
  text(".page-heading h1", title);
  text("#page-subtitle", subtitle);
  document.querySelectorAll(".nav-link, .bottom-nav-link").forEach(link => {
    const active = link.getAttribute("href") === path;
    link.classList.toggle("active", active);
    if (active) link.setAttribute("aria-current", "page");
    else link.removeAttribute("aria-current");
  });
}

async function navegarPara(nextPath, { push = true } = {}) {
  if (!pages[nextPath]) return;
  if (nextPath === path) {
    window.scrollTo({ top: 0, behavior: "smooth" });
    return;
  }
  path = nextPath;
  if (push) window.history.pushState({}, "", nextPath);
  document.querySelector("#conteudo").innerHTML = pages[path]();
  atualizarNavegacaoAtiva();
  window.scrollTo({ top: 0, behavior: "auto" });
  await initializeCurrentPage();
}

document.addEventListener("click", event => {
  const link = event.target.closest("a[href]");
  if (!link || event.defaultPrevented || event.button !== 0 || event.metaKey || event.ctrlKey || event.shiftKey || event.altKey) return;
  const target = new URL(link.href, window.location.href);
  const nextPath = ({ "/": "/", "/historico": "/historico", "/configuracoes": "/configuracoes" })[target.pathname.replace(/\/$/, "") || "/"];
  if (!nextPath || target.origin !== window.location.origin) return;
  event.preventDefault();
  void navegarPara(nextPath);
});

window.addEventListener("popstate", () => { void navegarPara(routeFromLocation(), { push: false }); });
void initializeCurrentPage();
startBackgroundRefresh();
