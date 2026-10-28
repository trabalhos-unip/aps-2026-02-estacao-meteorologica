import { number, parseDate, time } from "./ui.js?v=20261028-1";

function sampled(readings, field) {
  const valid = readings
    .filter(reading => Number.isFinite(Number(reading[field])) && reading[field] !== null && parseDate(reading.dataHoraColeta))
    .sort((a, b) => parseDate(a.dataHoraColeta) - parseDate(b.dataHoraColeta));
  if (valid.length <= 160) return valid;
  const step = (valid.length - 1) / 159;
  return Array.from({ length: 160 }, (_, index) => valid[Math.round(index * step)]);
}

function smoothPath(coordinates) {
  if (coordinates.length === 1) return `M${coordinates[0][0]} ${coordinates[0][1]}`;
  let path = `M${coordinates[0][0].toFixed(2)} ${coordinates[0][1].toFixed(2)}`;
  for (let index = 0; index < coordinates.length - 1; index++) {
    const previous = coordinates[Math.max(0, index - 1)];
    const current = coordinates[index];
    const next = coordinates[index + 1];
    const following = coordinates[Math.min(coordinates.length - 1, index + 2)];
    const gap = next[0] - current[0];
    const c1y = current[1] + (next[1] - previous[1]) / 6;
    const c2y = next[1] - (following[1] - current[1]) / 6;
    path += ` C${(current[0] + gap / 3).toFixed(2)} ${c1y.toFixed(2)} ${(next[0] - gap / 3).toFixed(2)} ${c2y.toFixed(2)} ${next[0].toFixed(2)} ${next[1].toFixed(2)}`;
  }
  return path;
}

let chartSequence = 0;
const renderedCharts = new Map();
let resizeFrame = 0;

window.addEventListener("resize", () => {
  cancelAnimationFrame(resizeFrame);
  resizeFrame = requestAnimationFrame(() => {
    for (const [selector, options] of renderedCharts) drawChart(selector, ...options);
  });
});

export function drawChart(selector, readings, field, suffix, color = "#2498ed", style = "line", domain = null) {
  const target = document.querySelector(selector);
  if (!target) return;
  renderedCharts.set(selector, [readings, field, suffix, color, style, domain]);
  const points = sampled(readings, field);
  if (!points.length) {
    target.innerHTML = '<div class="chart-empty">Sem leituras para desenhar o gráfico neste período.</div>';
    return;
  }

  const compact = target.classList.contains("small");
  const width = Math.max(compact ? 180 : 320, Math.round(target.clientWidth));
  const height = Math.max(compact ? 110 : 160, Math.round(target.clientHeight));
  const left = compact ? 42 : 38;
  const right = compact ? 8 : 7;
  const top = compact ? 22 : 23;
  const bottom = compact ? 25 : 25;
  const plotWidth = width - left - right;
  const plotHeight = height - top - bottom;
  const values = points.map(point => Number(point[field]));
  let minimum = Math.min(...values);
  let maximum = Math.max(...values);
  if (style === "bar") minimum = 0;
  const padding = Math.max((maximum - minimum) * 0.2, style === "bar" ? 1 : 2);
  if (suffix === "°C" && minimum >= 15 && maximum <= 35) [minimum, maximum] = [15, 35];
  else if (suffix === "%" && minimum >= 0 && maximum <= 100) [minimum, maximum] = [0, 100];
  else if (suffix === "km/h" && minimum >= 0 && maximum <= 40) [minimum, maximum] = [0, 40];
  else if (suffix === "mm" && minimum >= 0 && maximum <= 10) [minimum, maximum] = [0, 10];
  else {
    minimum = style === "bar" ? 0 : Math.floor((minimum - padding) / 5) * 5;
    maximum = Math.ceil((maximum + padding) / 5) * 5;
  }
  if (minimum === maximum) maximum = minimum + 1;

  const timestamps = points.map(point => parseDate(point.dataHoraColeta).getTime());
  const minTime = domain?.start instanceof Date ? domain.start.getTime() : Math.min(...timestamps);
  const maxTime = domain?.end instanceof Date ? domain.end.getTime() : Math.max(...timestamps);
  const daily = Boolean(domain?.daily || points.some(point => point.diario));
  const xAt = (timestamp, index) => left + (maxTime === minTime
    ? (points.length === 1 ? plotWidth / 2 : index / (points.length - 1) * plotWidth)
    : (timestamp - minTime) / (maxTime - minTime) * plotWidth);
  const yAt = value => top + (maximum - value) / (maximum - minimum) * plotHeight;
  const coordinates = points.map((point, index) => [xAt(timestamps[index], index), yAt(values[index])]);
  const line = smoothPath(coordinates);
  const area = `${line} L${coordinates.at(-1)[0].toFixed(2)} ${top + plotHeight} L${coordinates[0][0].toFixed(2)} ${top + plotHeight} Z`;
  const grid = Array.from({ length: 5 }, (_, index) => {
    const y = top + index * plotHeight / 4;
    const label = number(maximum - index * (maximum - minimum) / 4, 0);
    return `<line x1="${left}" y1="${y}" x2="${width - right}" y2="${y}" class="grid-line"/><text x="${left - 8}" y="${y + 4}" text-anchor="end" class="axis-label">${label}</text>`;
  }).join("");
  const tickCount = compact ? 5 : 9;
  const vertical = Array.from({ length: tickCount }, (_, index) => {
    const x = left + index * plotWidth / (tickCount - 1);
    const timestamp = new Date(minTime + index * (maxTime - minTime) / (tickCount - 1));
    const label = daily
      ? new Intl.DateTimeFormat("pt-BR", { day: "2-digit", month: "2-digit" }).format(timestamp)
      : time(timestamp).slice(0, 5);
    return `<line x1="${x}" y1="${top}" x2="${x}" y2="${top + plotHeight}" class="grid-line"/><text x="${x}" y="${height - 7}" text-anchor="middle" class="axis-label">${label}</text>`;
  }).join("");
  const gradientId = `chart-fill-${++chartSequence}`;
  let dataShape;
  if (style === "bar") {
    const barWidth = Math.max(4, Math.min(compact ? 13 : 18, plotWidth / Math.max(points.length * 2.2, 1)));
    dataShape = coordinates.map(([x, y]) => `<rect x="${(x - barWidth / 2).toFixed(2)}" y="${y.toFixed(2)}" width="${barWidth.toFixed(2)}" height="${(top + plotHeight - y).toFixed(2)}" rx="1.5" fill="${color}" opacity=".68"/>`).join("");
  } else {
    const fill = field === "velocidadeVento" ? "" : `<path d="${area}" fill="url(#${gradientId})"/>`;
    dataShape = `${fill}<path d="${line}" fill="none" stroke="${color}" stroke-width="${compact ? 1.8 : 2}" stroke-linecap="round" stroke-linejoin="round"/>`;
  }
  const highlighted = Math.round((points.length - 1) * .65);
  const [highlightX, highlightY] = coordinates[highlighted];
  target.innerHTML = `<svg class="plot" viewBox="0 0 ${width} ${height}" role="img" aria-label="Gráfico de ${suffix} com ${points.length} leituras"><defs><linearGradient id="${gradientId}" x1="0" y1="0" x2="0" y2="1"><stop offset="0%" stop-color="${color}" stop-opacity=".14"/><stop offset="100%" stop-color="${color}" stop-opacity=".015"/></linearGradient></defs><text x="2" y="12" class="axis-label">${suffix}</text>${grid}${vertical}${dataShape}<line class="chart-guide" x1="${highlightX}" x2="${highlightX}" y1="${top}" y2="${top + plotHeight}" stroke="${color}" stroke-width="1" stroke-dasharray="5 4" opacity=".55"/><circle class="chart-marker" cx="${highlightX}" cy="${highlightY}" r="5.5" fill="${color}" stroke="#fff" stroke-width="2"/></svg><div class="chart-tooltip" aria-hidden="true"><small></small><strong></strong></div>`;
  const guide = target.querySelector(".chart-guide");
  const marker = target.querySelector(".chart-marker");
  const tooltip = target.querySelector(".chart-tooltip");
  function showPoint(index) {
    const [x, y] = coordinates[index];
    guide.setAttribute("x1", x);
    guide.setAttribute("x2", x);
    marker.setAttribute("cx", x);
    marker.setAttribute("cy", y);
    tooltip.querySelector("small").textContent = daily
      ? new Intl.DateTimeFormat("pt-BR", { day: "2-digit", month: "2-digit", year: "numeric" }).format(parseDate(points[index].dataHoraColeta))
      : time(points[index].dataHoraColeta).slice(0, 5);
    tooltip.querySelector("strong").textContent = `${number(values[index])} ${suffix}`;
    tooltip.style.left = `${Math.min(width - 38, Math.max(38, x))}px`;
    tooltip.style.top = `${Math.max(42, y - 15)}px`;
  }
  showPoint(highlighted);
  target.onpointermove = event => {
    const bounds = target.getBoundingClientRect();
    const localX = (event.clientX - bounds.left) / bounds.width * width;
    let nearest = 0;
    for (let index = 1; index < coordinates.length; index++) {
      if (Math.abs(coordinates[index][0] - localX) < Math.abs(coordinates[nearest][0] - localX)) nearest = index;
    }
    showPoint(nearest);
  };
}

export function clearChart(selector, message) {
  renderedCharts.delete(selector);
  const target = document.querySelector(selector);
  if (target) {
    target.innerHTML = '<div class="chart-empty"></div>';
    target.firstElementChild.textContent = message;
  }
}

const scenarioSeries = {
  Temperatura: { suffix: "°C", max: 40, entries: [
    ["Tempo seco", "#ffa30b", [29, 31, 32, 31, 32, 30, 32]],
    ["Chuva intensa", "#1698f5", [12, 15, 16, 14, 19, 16, 15]],
    ["Vento forte", "#17bd7b", [21, 24, 23, 22, 25, 22, 24]],
  ] },
  Umidade: { suffix: "%", max: 100, entries: [
    ["Tempo seco", "#ffa30b", [31, 30, 28, 32, 30, 29, 30]],
    ["Chuva intensa", "#1698f5", [87, 89, 90, 92, 90, 91, 90]],
    ["Vento forte", "#17bd7b", [57, 59, 61, 60, 60, 62, 60]],
  ] },
  Precipitação: { suffix: "mm", max: 20, entries: [
    ["Tempo seco", "#ffa30b", [0, 0, 0, 0, 0, 0, 0]],
    ["Chuva intensa", "#1698f5", [10, 12, 14, 15, 15, 13, 15]],
    ["Vento forte", "#17bd7b", [0, 0, 0, 0, 0, 0, 0]],
  ] },
  Vento: { suffix: "km/h", max: 60, entries: [
    ["Tempo seco", "#ffa30b", [8, 10, 11, 9, 10, 9, 10]],
    ["Chuva intensa", "#1698f5", [16, 18, 19, 17, 18, 17, 18]],
    ["Vento forte", "#17bd7b", [45, 48, 51, 49, 50, 48, 50]],
  ] },
};

/** Gráfico de prévia, explicitamente ilustrativo até existir a API de simulações. */
export function drawScenarioComparison(selector, metric = "Temperatura") {
  const target = document.querySelector(selector);
  if (!target) return;
  const config = scenarioSeries[metric] || scenarioSeries.Temperatura;
  const width = 640, height = 214, left = 35, right = 10, top = 20, bottom = 34;
  const chartWidth = width - left - right, chartHeight = height - top - bottom;
  const xAt = index => left + index * chartWidth / 6;
  const yAt = value => top + (config.max - value) / config.max * chartHeight;
  const horizontal = Array.from({length:5}, (_, index) => {
    const y = top + index * chartHeight / 4;
    return `<line x1="${left}" y1="${y}" x2="${width-right}" y2="${y}" class="grid-line"/><text x="${left-10}" y="${y+4}" text-anchor="end" class="axis-label">${number(config.max-index*config.max/4,0)}</text>`;
  }).join("");
  const vertical = Array.from({length:7}, (_, index) => {
    const x=xAt(index);
    return `<line x1="${x}" y1="${top}" x2="${x}" y2="${top+chartHeight}" class="grid-line"/><text x="${x}" y="${height-7}" text-anchor="middle" class="axis-label">${10+index}h</text>`;
  }).join("");
  const paths = config.entries.map(([name,color,values], seriesIndex) => {
    const coordinates = values.map((value,index) => [xAt(index), yAt(value)]);
    const line = smoothPath(coordinates);
    const fill = seriesIndex === 1 ? `<path d="${line} L${xAt(6)} ${top+chartHeight} L${xAt(0)} ${top+chartHeight} Z" fill="${color}" opacity=".08"/>` : "";
    return `${fill}<path d="${line}" fill="none" stroke="${color}" stroke-width="2.2" stroke-linecap="round"/>`;
  }).join("");
  const markers = config.entries.map(([, color, values]) => `<circle class="scenario-marker" cx="${xAt(4)}" cy="${yAt(values[4])}" r="5" fill="${color}" stroke="#fff" stroke-width="2"/>`).join("");
  target.innerHTML = `<svg class="plot" viewBox="0 0 ${width} ${height}" role="img" aria-label="Prévia ilustrativa de ${metric.toLowerCase()} dos três cenários"><text x="2" y="12" class="axis-label">${config.suffix}</text>${horizontal}${vertical}${paths}<line class="chart-guide" x1="${xAt(4)}" x2="${xAt(4)}" y1="${top}" y2="${top+chartHeight}" stroke="#1598ed" stroke-width="1" stroke-dasharray="5 4"/>${markers}</svg><div class="scenario-chart-tip" aria-hidden="true"></div>`;
  const tip=target.querySelector(".scenario-chart-tip");
  const guide=target.querySelector(".chart-guide");
  const circles=target.querySelectorAll(".scenario-marker");
  function show(index) {
    const x=xAt(index);
    guide.setAttribute("x1",x);guide.setAttribute("x2",x);
    circles.forEach((circle,seriesIndex)=>{circle.setAttribute("cx",x);circle.setAttribute("cy",yAt(config.entries[seriesIndex][2][index]));});
    tip.replaceChildren();
    const heading=document.createElement("small");heading.textContent=`${10+index}:00`;tip.append(heading);
    for(const [name,color,values] of config.entries){const row=document.createElement("div");const dot=document.createElement("i");dot.style.background=color;const label=document.createElement("span");label.textContent=name;const value=document.createElement("strong");value.textContent=`${number(values[index],metric==="Temperatura"?1:0)} ${config.suffix}`;row.append(dot,label,value);tip.append(row);}
    tip.style.left=`${Math.min(79,Math.max(11,x/width*100))}%`;
  }
  show(4);
  target.onpointermove=event=>{const bounds=target.getBoundingClientRect();const x=(event.clientX-bounds.left)/bounds.width*width;show(Math.max(0,Math.min(6,Math.round((x-left)/chartWidth*6))));};
}
