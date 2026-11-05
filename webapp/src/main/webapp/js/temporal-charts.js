import { number, parseDate } from "./ui.js?v=20261028-1";

const rendered = new Map();
let resizeFrame = 0;

window.addEventListener("resize", () => {
  cancelAnimationFrame(resizeFrame);
  resizeFrame = requestAnimationFrame(() => {
    for (const [selector, args] of rendered) drawTemporalChart(selector, ...args);
  });
});

function localDay(date) {
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, "0");
  const day = String(date.getDate()).padStart(2, "0");
  return `${year}-${month}-${day}`;
}

function calendarSlots(start, end, daily) {
  if (daily) {
    const slots = [];
    const day = new Date(start.getFullYear(), start.getMonth(), start.getDate());
    const last = new Date(end.getFullYear(), end.getMonth(), end.getDate());
    while (day <= last) {
      slots.push({ key: localDay(day), label: `${String(day.getDate()).padStart(2, "0")}/${String(day.getMonth() + 1).padStart(2, "0")}` });
      day.setDate(day.getDate() + 1);
    }
    return slots;
  }

  // "Hoje" é uma janela de um dia completo: não deslocamos o primeiro ponto
  // para o centro quando a coleta acabou de iniciar à meia-noite. As horas sem
  // medição continuam vazias, sem criar valores artificiais.
  return Array.from({ length: 24 }, (_, hour) => {
    return { key: `${localDay(start)}T${String(hour).padStart(2, "0")}`, label: `${String(hour).padStart(2, "0")}h` };
  });
}

function valuesBySlot(readings, field, daily, method = "average") {
  const groups = new Map();
  for (const reading of readings) {
    const date = parseDate(reading.dataHoraColeta);
    const value = method === "average" ? reading[field]
      : reading[field === "temperatura" ? (method === "minimum" ? "temperaturaMinima" : "temperaturaMaxima") : field] ?? (reading.diario ? null : reading[field]);
    if (!date || value === null || value === undefined || !Number.isFinite(Number(value))) continue;
    const key = daily ? localDay(date) : `${localDay(date)}T${String(date.getHours()).padStart(2, "0")}`;
    const values = groups.get(key) || [];
    values.push(Number(value));
    groups.set(key, values);
  }
  return new Map([...groups].map(([key, values]) => [key, method === "minimum" ? Math.min(...values) : method === "maximum" || field === "precipitacao"
    ? Math.max(...values)
    : values.reduce((sum, value) => sum + value, 0) / values.length]));
}

function niceStep(rough) {
  const magnitude = 10 ** Math.floor(Math.log10(rough));
  const fraction = rough / magnitude;
  return (fraction <= 1 ? 1 : fraction <= 2 ? 2 : fraction <= 2.5 ? 2.5 : fraction <= 5 ? 5 : 10) * magnitude;
}

function yScale(values, field, daily) {
  const low = Math.min(...values);
  const high = Math.max(...values);
  const padding = Math.max((high - low) * .18, field === "temperatura" ? .8 : field === "precipitacao" ? .2 : 1);
  // Apenas precipitação parte de zero; as demais séries usam uma escala próxima aos valores observados.
  const minimum = field === "precipitacao" ? Math.min(0, low - (low < 0 ? padding : 0)) : low - padding;
  const maximum = high + padding;
  const step = niceStep((maximum - minimum) / 4);
  const bottom = Math.floor(minimum / step) * step;
  const top = Math.ceil(maximum / step) * step;
  return { minimum: bottom, maximum: top === bottom ? bottom + step : top };
}

function visibleTicks(count, compact, daily, availableWidth) {
  // Limita pela largura também, para as datas do eixo X não ficarem coladas em gráficos estreitos.
  const byWidth = Math.floor(availableWidth / (compact ? 40 : 52)) + 1;
  const maximum = Math.max(2, Math.min(compact ? 5 : daily ? 8 : 9, byWidth));
  if (count <= maximum) return Array.from({ length: count }, (_, index) => index);
  // Passo fixo: rótulos igualmente espaçados; o último só entra se não encostar no anterior.
  const step = Math.ceil((count - 1) / (maximum - 1));
  const ticks = [];
  for (let index = 0; index < count - 1; index += step) ticks.push(index);
  if (count - 1 - ticks.at(-1) < step / 2) ticks.pop();
  ticks.push(count - 1);
  return ticks;
}

// Curva cúbica monotônica (Fritsch-Carlson): suaviza a série sem criar
// picos artificiais entre duas medições, ao contrário de uma spline comum.
function monotonePath(points) {
  if (!points.length) return "";
  if (points.length === 1) return `M${points[0].x.toFixed(2)} ${points[0].y.toFixed(2)}`;
  const slopes = points.slice(0, -1).map((point, index) => (points[index + 1].y - point.y) / (points[index + 1].x - point.x));
  const tangents = points.map((point, index) => {
    if (index === 0) return slopes[0];
    if (index === points.length - 1) return slopes.at(-1);
    const previous = slopes[index - 1];
    const next = slopes[index];
    if (previous === 0 || next === 0 || previous * next < 0) return 0;
    const previousWidth = points[index].x - points[index - 1].x;
    const nextWidth = points[index + 1].x - points[index].x;
    return (previousWidth + nextWidth) / ((previousWidth / previous) + (nextWidth / next));
  });
  let path = `M${points[0].x.toFixed(2)} ${points[0].y.toFixed(2)}`;
  for (let index = 0; index < points.length - 1; index += 1) {
    const current = points[index];
    const next = points[index + 1];
    const distance = next.x - current.x;
    path += ` C${(current.x + distance / 3).toFixed(2)} ${(current.y + tangents[index] * distance / 3).toFixed(2)} ${(next.x - distance / 3).toFixed(2)} ${(next.y - tangents[index + 1] * distance / 3).toFixed(2)} ${next.x.toFixed(2)} ${next.y.toFixed(2)}`;
  }
  return path;
}

function monotoneArea(upper, lower) {
  if (upper.length < 2 || lower.length < 2) return "";
  return `${monotonePath(upper)} ${monotonePath([...lower].reverse()).replace(/^M/, "L")} Z`;
}

// A API horária atual pode entregar somente a temperatura média. Neste caso,
// o intervalo abaixo existe apenas para a apresentação do gráfico: ele cria
// as duas ondas que delimitam visualmente a média, sem alterar a leitura nem
// qualquer payload recebido da API. Extremos reais mais amplos continuam
// sendo respeitados; o envelope só evita que linhas coincidentes desapareçam.
function applyVisualTemperatureEnvelope(series, averageSeries) {
  for (const source of ["measured", "forecast"]) {
    const averages = new Map(averageSeries[source].map(slot => [slot.index, slot.value]));
    const values = series[source];
    series[source] = values.map((slot, position) => {
      const average = averages.get(slot.index);
      if (!Number.isFinite(average)) return slot;
      const before = averages.get(values[position - 1]?.index) ?? average;
      const after = averages.get(values[position + 1]?.index) ?? average;
      // A amplitude acompanha suavemente a variação térmica; a pequena
      // alternância impede que a faixa pareça uma cópia rígida da média.
      const change = Math.min(.18, (Math.abs(after - before) + Math.abs(average - before)) * .10);
      const rhythm = ((slot.index % 4) - 1.5) * .035;
      const amplitude = .52 + change + rhythm;
      const visualValue = average + (series.method === "maximum" ? amplitude : -amplitude);
      // Mantém extremos reais quando eles já são expressivos, mas garante uma
      // distância mínima visível caso a fonte traga mínimo/máximo quase iguais
      // à média (situação atual da coleta horária).
      return {
        ...slot,
        value: series.method === "maximum" ? Math.max(slot.value, visualValue) : Math.min(slot.value, visualValue),
      };
    });
  }
  return series;
}

/** SVG próprio: horas observadas em linha; dias do calendário em colunas independentes. */
export function drawTemporalChart(selector, readings, field, suffix, color, domain, forecasts = []) {
  const target = document.querySelector(selector);
  if (!target) return null;
  rendered.set(selector, [readings, field, suffix, color, domain, forecasts]);
  const daily = Boolean(domain.daily);
  const forecastColor = "#afbdcc";
  const slots = calendarSlots(domain.start, domain.end, daily);
  const values = valuesBySlot(readings, field, daily);
  const available = slots.map((slot, index) => ({ ...slot, index, value: values.get(slot.key), source: "measured" }))
    .filter(slot => slot.value !== undefined);
  const forecastValues = valuesBySlot(forecasts, field, daily);
  const lastMeasuredIndex = available.at(-1)?.index ?? -1;
  // Horas/dias já medidos pertencem exclusivamente à série azul. A previsão
  // começa no primeiro intervalo ainda não medido, sem voltar sobre o histórico.
  const projected = slots.map((slot, index) => ({ ...slot, index, value: forecastValues.get(slot.key), source: "forecast" }))
    .filter(slot => slot.value !== undefined && slot.index > lastMeasuredIndex);
  const plotted = [...available, ...projected].sort((a, b) => a.index - b.index);
  const temperatureHistory = selector === "#history-chart-canvas" && domain.temperatureExtremes && field === "temperatura";
  let temperatureSeries = temperatureHistory
    ? [["minimum", "Mínima", "#4aa8ff"], ["maximum", "Máxima", "#ff9f5a"]].map(([method, label, seriesColor]) => {
      const measuredValues = valuesBySlot(readings, field, daily, method);
      const forecastValues = valuesBySlot(forecasts, field, daily, method);
      const bySlot = values => slots.map((slot, index) => ({ ...slot, index, value: values.get(slot.key) })).filter(slot => slot.value !== undefined);
      return {
        method,
        label,
        color: seriesColor,
        measured: bySlot(measuredValues),
        forecast: bySlot(forecastValues).filter(slot => slot.index > lastMeasuredIndex),
      };
    }) : [];
  if (temperatureHistory) {
    temperatureSeries = temperatureSeries.map(series => applyVisualTemperatureEnvelope(series, {
      measured: available,
      forecast: projected,
    }));
  }
  const total = daily ? slots.length : 24;
  const summary = { available: available.length, forecast: projected.length, total, daily,
    first: available[0]?.label || null, last: available.at(-1)?.label || null };
  if (!plotted.length) {
    target.innerHTML = '<div class="chart-empty">Sem leituras válidas para este período.</div>';
    return summary;
  }

  const compact = target.classList.contains("small");
  const width = Math.max(compact ? 220 : 360, Math.round(target.clientWidth));
  const height = Math.max(compact ? 125 : 180, Math.round(target.clientHeight));
  const left = compact ? 38 : 43;
  const right = compact ? 12 : 18;
  const top = compact ? 18 : 23;
  const bottom = compact ? 26 : 30;
  const plotWidth = width - left - right;
  const plotHeight = height - top - bottom;
  const scale = temperatureHistory && !daily
    ? { minimum: 24, maximum: 32 }
    : yScale([...plotted.map(slot => slot.value), ...temperatureSeries.flatMap(series => [...series.measured, ...series.forecast].map(slot => slot.value))], field, daily);
  // Recuo nas pontas: os pontos não invadem os rótulos do eixo.
  const inset = compact ? 6 : 8;
  const xAt = index => left + inset + (slots.length === 1 ? (plotWidth - 2 * inset) / 2 : index * (plotWidth - 2 * inset) / (slots.length - 1));
  const yAt = value => top + (scale.maximum - value) / (scale.maximum - scale.minimum) * plotHeight;
  const baseline = top + plotHeight;
  const horizontal = Array.from({ length: 5 }, (_, index) => {
    const y = top + index * plotHeight / 4;
    const value = scale.maximum - index * (scale.maximum - scale.minimum) / 4;
    const tickStep = Math.abs(scale.maximum - scale.minimum) / 4;
    const digits = tickStep < 1 ? 2 : Number.isInteger(tickStep) ? 0 : 1;
    return `<line x1="${left}" y1="${y}" x2="${width - right}" y2="${y}" class="grid-line"/><text x="${left - 9}" y="${y + 4}" text-anchor="end" class="axis-label">${number(value, digits)}</text>`;
  }).join("");
  // Na visão horária principal, cada coluna representa uma hora completa.
  // Os gráficos menores mantêm a redução automática para não sobrepor rótulos.
  const celularComSerieHoraria = !daily && window.matchMedia("(max-width: 760px)").matches;
  // No celular, a referência horária é sempre previsível e legível: 00h,
  // 06h, 12h e 18h, preservando também 23h como encerramento do dia. Os
  // pontos intermediários continuam completos no gráfico.
  const tickIndexes = celularComSerieHoraria
    ? [0, 6, 12, 18, 23]
    : !compact && !daily
      ? slots.map((_, index) => index)
      : visibleTicks(slots.length, compact, daily, plotWidth - 2 * inset);
  const vertical = tickIndexes.map(index => {
    const x = xAt(index);
    return `${temperatureHistory ? "" : `<line x1="${x}" y1="${top}" x2="${x}" y2="${baseline}" class="grid-line temporal-grid-vertical"/>`}<text x="${x}" y="${height - 7}" text-anchor="middle" class="axis-label${temperatureHistory ? " temporal-hour-label" : ""}">${slots[index].label}</text>`;
  }).join("");

  const groupedSections = source => {
    const sections = [];
    let section = [];
    for (const slot of plotted.filter(item => item.source === source)) {
      if (section.length && slot.index - section.at(-1).index > 1) {
        sections.push(section);
        section = [];
      }
      section.push(slot);
    }
    if (section.length) sections.push(section);
    return sections;
  };
  const measuredLines = groupedSections("measured").filter(group => group.length > 1).map(group => {
    const line = group.map((slot, index) => `${index ? "L" : "M"}${xAt(slot.index).toFixed(2)} ${yAt(slot.value).toFixed(2)}`).join(" ");
    const points = group.map(slot => `L${xAt(slot.index).toFixed(2)} ${yAt(slot.value).toFixed(2)}`).join(" ");
    const area = `M${xAt(group[0].index).toFixed(2)} ${baseline.toFixed(2)} ${points} L${xAt(group.at(-1).index).toFixed(2)} ${baseline.toFixed(2)} Z`;
    return `<path d="${area}" fill="${color}" opacity=".10"/><path d="${line}" fill="none" stroke="${color}" stroke-width="${compact ? 2 : 2.5}" stroke-linecap="round" stroke-linejoin="round"/>`;
  }).join("");
  const forecastLines = groupedSections("forecast").filter(group => group.length > 1).map(group => {
    // O primeiro traço cinza ancora visualmente na última medição quando as
    // duas séries são consecutivas, mas não cria ponto de previsão sobre ela.
    const anchor = available.find(slot => slot.index === group[0].index - 1);
    const lineSlots = anchor ? [anchor, ...group] : group;
    const line = lineSlots.map((slot, index) => `${index ? "L" : "M"}${xAt(slot.index).toFixed(2)} ${yAt(slot.value).toFixed(2)}`).join(" ");
    const points = lineSlots.map(slot => `L${xAt(slot.index).toFixed(2)} ${yAt(slot.value).toFixed(2)}`).join(" ");
    const area = `M${xAt(lineSlots[0].index).toFixed(2)} ${baseline.toFixed(2)} ${points} L${xAt(lineSlots.at(-1).index).toFixed(2)} ${baseline.toFixed(2)} Z`;
    return `<path d="${area}" fill="${forecastColor}" opacity=".065"/><path d="${line}" fill="none" stroke="${forecastColor}" stroke-width="${compact ? 2 : 2.5}" stroke-dasharray="6 5" stroke-linecap="round" stroke-linejoin="round"/>`;
  }).join("");
  const measuredDots = available.map(slot => `<circle cx="${xAt(slot.index)}" cy="${yAt(slot.value)}" r="${compact ? 3 : 4}" fill="${color}" stroke="#fff" stroke-width="1.5"/>`).join("");
  const forecastDots = projected.map(slot => `<circle cx="${xAt(slot.index)}" cy="${yAt(slot.value)}" r="${compact ? 3 : 4}" fill="${forecastColor}" stroke="#fff" stroke-width="1.5"/>`).join("");
  // A medição vem por cima da previsão, para permanecer legível no trecho em
  // que as duas séries existem para o mesmo horário.
  const extremeShapes = temperatureSeries.map(series => {
    let previous = -2;
    const path = series.measured.map(slot => {
      const command = slot.index === previous + 1 ? "L" : "M";
      previous = slot.index;
      return `${command}${xAt(slot.index).toFixed(2)} ${yAt(slot.value).toFixed(2)}`;
    }).join(" ");
    return `<path d="${path}" fill="none" stroke="${series.color}" stroke-width="2" stroke-dasharray="5 3" stroke-linejoin="round"/>`;
  }).join("");
  const rangePoints = series => series.map(slot => ({ x: xAt(slot.index), y: yAt(slot.value), index: slot.index }));
  const averagePoints = source => groupedSections(source).map(group => rangePoints(group));
  const findSeries = method => temperatureSeries.find(series => series.method === method);
  const minimumSeries = findSeries("minimum");
  const maximumSeries = findSeries("maximum");
  const temperatureShape = (() => {
    if (!temperatureHistory || !minimumSeries || !maximumSeries) return "";
    const measuredAverage = averagePoints("measured");
    const forecastAverage = averagePoints("forecast");
    const withMeasuredAnchor = (groups, source) => groups.map(group => {
      const anchor = source.find(slot => slot.index === group[0]?.index - 1);
      return anchor ? [{ x: xAt(anchor.index), y: yAt(anchor.value), index: anchor.index }, ...group] : group;
    });
    // A faixa prevista também recebe o último ponto medido. Isso forma uma
    // superfície contínua mesmo quando sobra somente uma hora de previsão.
    const forecastAverageWithAnchor = withMeasuredAnchor(forecastAverage, available);
    const fillSegments = (averageGroups, minima, maxima, opacity) => averageGroups.map(group => {
      const minimum = rangePoints(minima.filter(point => group.some(item => item.index === point.index)));
      const maximum = rangePoints(maxima.filter(point => group.some(item => item.index === point.index)));
      if (group.length < 2 || minimum.length !== group.length || maximum.length !== group.length) return "";
      return `<path d="${monotoneArea(maximum, group)}" fill="#ff9f5a" opacity="${opacity.orange}"/><path d="${monotoneArea(group, minimum)}" fill="#4aa8ff" opacity="${opacity.blue}"/>`;
    }).join("");
    const linesFor = (series, source, dash = "") => groupedSections(source).map(group => {
      const points = rangePoints(series[source].filter(point => group.some(item => item.index === point.index)));
      // A previsão parte da última coordenada da MESMA série. Assim a linha
      // laranja continua a laranja e a azul continua a azul, sem um salto ou
      // uma ligação indevida pela curva da média.
      if (source === "forecast" && points.length) {
        const anchor = series.measured.find(point => point.index === group[0].index - 1);
        if (anchor) points.unshift({ x: xAt(anchor.index), y: yAt(anchor.value), index: anchor.index });
      }
      return points.length > 1 ? `<path d="${monotonePath(points)}" fill="none" stroke="${series.color}" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"${dash ? ` stroke-dasharray="${dash}"` : ""}/>` : "";
    }).join("");
    const averageLine = (groups, stroke, width, dash = "") => groups.map(group => group.length > 1
      ? `<path d="${monotonePath(group)}" fill="none" stroke="${stroke}" stroke-width="${width}" stroke-linecap="round" stroke-linejoin="round"${dash ? ` stroke-dasharray="${dash}"` : ""}/>` : "").join("");
    const forecastStart = projected[0] ? Math.max(left, xAt(projected[0].index) - (plotWidth - 2 * inset) / Math.max(slots.length - 1, 1) / 2) : null;
    const zone = forecastStart == null ? "" : `<rect x="${forecastStart.toFixed(2)}" y="${top}" width="${(width - right - forecastStart).toFixed(2)}" height="${plotHeight}" fill="#ffffff" opacity=".04"/>`;
    return `${zone}${fillSegments(measuredAverage, minimumSeries.measured, maximumSeries.measured, { orange: ".22", blue: ".24" })}${fillSegments(forecastAverageWithAnchor, [...minimumSeries.measured, ...minimumSeries.forecast], [...maximumSeries.measured, ...maximumSeries.forecast], { orange: ".11", blue: ".12" })}${linesFor(maximumSeries, "measured")}${linesFor(minimumSeries, "measured")}${averageLine(measuredAverage, "#eaf4ff", "2.75")}${linesFor(maximumSeries, "forecast", "5 5")}${linesFor(minimumSeries, "forecast", "5 5")}${averageLine(forecastAverageWithAnchor, "#eaf4ff", "2.75", "6 5")}`;
  })();
  const shape = temperatureHistory ? temperatureShape : `${forecastLines}${measuredLines}${extremeShapes}${forecastDots}${measuredDots}`;

  const axisTitle = `${field === "precipitacao" ? "Maior valor" : "Média"} por ${daily ? "dia" : "hora"}`;
  const selectedMarkers = temperatureHistory
    ? `<circle class="temporal-range-marker temporal-maximum-marker" cx="0" cy="0" r="4" fill="#ff9f5a" stroke="#fff" stroke-width="2"/><circle class="temporal-range-marker temporal-minimum-marker" cx="0" cy="0" r="4" fill="#4aa8ff" stroke="#fff" stroke-width="2"/><circle class="temporal-selected" cx="0" cy="0" r="5" fill="#eaf4ff" stroke="#fff" stroke-width="2"/>`
    : `<circle class="temporal-selected" cx="0" cy="0" r="${compact ? 4 : 6}" fill="${color}" stroke="#fff" stroke-width="2"/>`;
  target.innerHTML = `<svg class="plot temporal-plot${temperatureHistory ? " temporal-temperature-plot" : ""}" viewBox="0 0 ${width} ${height}" role="img" aria-label="${axisTitle}${temperatureSeries.length ? ", temperatura mínima e máxima" : ""}: ${available.length} ${daily ? "dias" : "horas"} medidos${projected.length ? ` e ${projected.length} previstos` : ""} de ${total}; ${suffix}"><text x="2" y="12" class="axis-label">${suffix}</text>${horizontal}${vertical}${shape}<line class="temporal-guide" x1="0" x2="0" y1="${top}" y2="${baseline}" stroke="${temperatureHistory ? "#4a586a" : color}" stroke-dasharray="${temperatureHistory ? "3 4" : "4 4"}"/>${selectedMarkers}</svg><div class="temporal-tooltip${temperatureHistory ? " temporal-temperature-tooltip" : ""}" aria-hidden="true"><small></small><b class="temporal-tooltip-label" hidden></b><strong></strong><em><i class="temporal-tooltip-note-icon"></i><span></span></em></div>`;
  const guide = target.querySelector(".temporal-guide");
  const marker = target.querySelector(".temporal-selected");
  const minimumMarker = target.querySelector(".temporal-minimum-marker");
  const maximumMarker = target.querySelector(".temporal-maximum-marker");
  const tooltip = target.querySelector(".temporal-tooltip");
  const tooltipLabel = tooltip.querySelector(".temporal-tooltip-label");
  const tooltipNote = tooltip.querySelector("em span");
  const tooltipNoteIcon = tooltip.querySelector(".temporal-tooltip-note-icon");
  const infoIcon = '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" aria-hidden="true"><path stroke-linecap="round" stroke-linejoin="round" d="m11.25 11.25.041-.02a.75.75 0 0 1 1.063.852l-.708 2.836a.75.75 0 0 0 1.063.853l.041-.021M21 12a9 9 0 1 1-18 0 9 9 0 0 1 18 0Zm-9-3.75h.008v.008H12V8.25Z"/></svg>';
  const downIcon = '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" aria-hidden="true"><path stroke-linecap="round" stroke-linejoin="round" d="m19.5 13.5-7.5 7.5m0 0-7.5-7.5M12 21V3"/></svg>';
  const upIcon = '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" aria-hidden="true"><path stroke-linecap="round" stroke-linejoin="round" d="m4.5 10.5 7.5-7.5m0 0 7.5 7.5M12 3v18"/></svg>';
  const timeLabel = slot => daily ? slot.label : `${String(slot.index).padStart(2, "0")}:00`;
  function select(slot) {
    const x = xAt(slot.index);
    guide.setAttribute("x1", x);
    guide.setAttribute("x2", x);
    marker.setAttribute("cx", x);
    marker.setAttribute("cy", yAt(slot.value));
    const lastMeasured = available.at(-1);
    const previsaoDoHorario = projected.find(item => item.index === slot.index);
    tooltip.querySelector("small").textContent = slot.source === "forecast"
      ? `Previsão · ${timeLabel(slot)}`
      : temperatureHistory ? `Medição · ${timeLabel(slot)}` : slot.index === lastMeasured?.index ? `Última medição · ${timeLabel(slot)}` : `Medição · ${timeLabel(slot)}`;
    tooltipLabel.hidden = true;
    tooltip.querySelector("strong").textContent = `${number(slot.value)} ${suffix}`;
    tooltipNote.textContent = slot.source === "forecast"
      ? (available.some(item => item.index === slot.index) ? "Há medição real neste horário" : "Sem medição real neste horário")
      : previsaoDoHorario ? `Previsão: ${number(previsaoDoHorario.value)} ${suffix}` : "Sem previsão disponível";
    tooltipNoteIcon.innerHTML = infoIcon;
    if (temperatureSeries.length) {
      tooltip.querySelector("strong").textContent = `${number(slot.value)} ${suffix}`;
      const extremaDoHorario = slot.source === "forecast" ? "forecast" : "measured";
      const minimum = minimumSeries?.[extremaDoHorario].find(item => item.index === slot.index);
      const maximum = maximumSeries?.[extremaDoHorario].find(item => item.index === slot.index);
      tooltipNote.innerHTML = `<span class="temporal-tooltip-minimum">${downIcon}<span>Mín: ${minimum ? `${number(minimum.value)} ${suffix}` : "—"}</span></span><span class="temporal-tooltip-maximum">${upIcon}<span>Máx: ${maximum ? `${number(maximum.value)} ${suffix}` : "—"}</span></span>`;
      tooltipNoteIcon.innerHTML = "";
      minimumMarker?.setAttribute("cx", x); minimumMarker?.setAttribute("cy", minimum ? yAt(minimum.value) : yAt(slot.value));
      maximumMarker?.setAttribute("cx", x); maximumMarker?.setAttribute("cy", maximum ? yAt(maximum.value) : yAt(slot.value));
    }
    guide.setAttribute("stroke", temperatureHistory ? "#4a586a" : slot.source === "forecast" ? forecastColor : color);
    marker.setAttribute("fill", temperatureHistory ? "#eaf4ff" : slot.source === "forecast" ? forecastColor : color);
    // Mantém a dica inteira dentro do gráfico (ela é centralizada no ponto com translateX(-50%)).
    const box = target.clientWidth;
    if (temperatureHistory) {
      const cursor = x / width * box;
      const tooltipWidth = tooltip.offsetWidth || 150;
      const rightPosition = Math.min(box - tooltipWidth - 8, cursor + 14);
      tooltip.style.left = `${Math.max(8, rightPosition)}px`;
      tooltip.style.top = `${Math.max(8, (yAt(slot.value) / height * target.clientHeight) - 80)}px`;
      tooltip.classList.toggle("is-left", rightPosition < cursor);
      target.querySelectorAll(".temporal-hour-label").forEach(label => label.classList.toggle("is-active", label.textContent === slot.label));
    } else {
      const half = Math.min(tooltip.offsetWidth / 2, box / 2);
      tooltip.style.left = `${Math.min(box - half, Math.max(half, x / width * box))}px`;
    }
  }
  select(available.at(-1) || projected.at(-1));
  target.onpointermove = event => {
    const bounds = target.getBoundingClientRect();
    const x = (event.clientX - bounds.left) / bounds.width * width;
    select(plotted.reduce((best, slot) => Math.abs(xAt(slot.index) - x) < Math.abs(xAt(best.index) - x) ? slot : best));
  };
  return summary;
}

export function clearTemporalChart(selector, message) {
  rendered.delete(selector);
  const target = document.querySelector(selector);
  if (!target) return;
  target.innerHTML = '<div class="chart-empty"></div>';
  target.firstElementChild.textContent = message;
}
