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

function valuesBySlot(readings, field, daily) {
  const groups = new Map();
  for (const reading of readings) {
    const date = parseDate(reading.dataHoraColeta);
    const value = reading[field];
    if (!date || value === null || value === undefined || !Number.isFinite(Number(value))) continue;
    const key = daily ? localDay(date) : `${localDay(date)}T${String(date.getHours()).padStart(2, "0")}`;
    const values = groups.get(key) || [];
    values.push(Number(value));
    groups.set(key, values);
  }
  return new Map([...groups].map(([key, values]) => [key, field === "precipitacao"
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

/** SVG próprio: horas observadas em linha; dias do calendário em colunas independentes. */
export function drawTemporalChart(selector, readings, field, suffix, color, domain) {
  const target = document.querySelector(selector);
  if (!target) return null;
  rendered.set(selector, [readings, field, suffix, color, domain]);
  const daily = Boolean(domain.daily);
  const slots = calendarSlots(domain.start, domain.end, daily);
  const values = valuesBySlot(readings, field, daily);
  const available = slots.map((slot, index) => ({ ...slot, index, value: values.get(slot.key) }))
    .filter(slot => slot.value !== undefined);
  const total = daily ? slots.length : 24;
  const summary = { available: available.length, total, daily,
    first: available[0]?.label || null, last: available.at(-1)?.label || null };
  if (!available.length) {
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
  const scale = yScale(available.map(slot => slot.value), field, daily);
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
  const vertical = visibleTicks(slots.length, compact, daily, plotWidth - 2 * inset).map(index => {
    const x = xAt(index);
    return `<line x1="${x}" y1="${top}" x2="${x}" y2="${baseline}" class="grid-line temporal-grid-vertical"/><text x="${x}" y="${height - 7}" text-anchor="middle" class="axis-label">${slots[index].label}</text>`;
  }).join("");

  const sections = [];
  let section = [];
  for (const slot of available) {
    if (section.length && slot.index - section.at(-1).index > 1) {
      sections.push(section);
      section = [];
    }
    section.push(slot);
  }
  if (section.length) sections.push(section);
  const lines = sections.filter(group => group.length > 1).map(group => {
    const line = group.map((slot, index) => `${index ? "L" : "M"}${xAt(slot.index).toFixed(2)} ${yAt(slot.value).toFixed(2)}`).join(" ");
    const points = group.map(slot => `L${xAt(slot.index).toFixed(2)} ${yAt(slot.value).toFixed(2)}`).join(" ");
    const area = `M${xAt(group[0].index).toFixed(2)} ${baseline.toFixed(2)} ${points} L${xAt(group.at(-1).index).toFixed(2)} ${baseline.toFixed(2)} Z`;
    return `<path d="${area}" fill="${color}" opacity=".10"/><path d="${line}" fill="none" stroke="${color}" stroke-width="${compact ? 2 : 2.5}" stroke-linecap="round" stroke-linejoin="round"/>`;
  }).join("");
  const dots = available.map(slot => `<circle cx="${xAt(slot.index)}" cy="${yAt(slot.value)}" r="${compact ? 3 : 4}" fill="${color}" stroke="#fff" stroke-width="1.5"/>`).join("");
  const shape = `${lines}${dots}`;

  const axisTitle = `${field === "precipitacao" ? "Maior valor" : "Média"} por ${daily ? "dia" : "hora"}`;
  target.innerHTML = `<svg class="plot temporal-plot" viewBox="0 0 ${width} ${height}" role="img" aria-label="${axisTitle}: ${available.length} ${daily ? "dias" : "horas"} com dados de ${total}; ${suffix}"><text x="2" y="12" class="axis-label">${suffix}</text>${horizontal}${vertical}${shape}<line class="temporal-guide" x1="0" x2="0" y1="${top}" y2="${baseline}" stroke="${color}" stroke-dasharray="4 4"/><circle class="temporal-selected" cx="0" cy="0" r="${compact ? 4 : 6}" fill="${color}" stroke="#fff" stroke-width="2"/></svg><div class="temporal-tooltip" aria-hidden="true"><small></small><strong></strong></div>`;
  const guide = target.querySelector(".temporal-guide");
  const marker = target.querySelector(".temporal-selected");
  const tooltip = target.querySelector(".temporal-tooltip");
  function select(slot) {
    const x = xAt(slot.index);
    guide.setAttribute("x1", x);
    guide.setAttribute("x2", x);
    marker.setAttribute("cx", x);
    marker.setAttribute("cy", yAt(slot.value));
    tooltip.querySelector("small").textContent = `${slot.label} · ${axisTitle.toLowerCase()}`;
    tooltip.querySelector("strong").textContent = `${number(slot.value)} ${suffix}`;
    // Mantém a dica inteira dentro do gráfico (ela é centralizada no ponto com translateX(-50%)).
    const box = target.clientWidth;
    const half = Math.min(tooltip.offsetWidth / 2, box / 2);
    tooltip.style.left = `${Math.min(box - half, Math.max(half, x / width * box))}px`;
  }
  select(available.at(-1));
  target.onpointermove = event => {
    const bounds = target.getBoundingClientRect();
    const x = (event.clientX - bounds.left) / bounds.width * width;
    select(available.reduce((best, slot) => Math.abs(xAt(slot.index) - x) < Math.abs(xAt(best.index) - x) ? slot : best));
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
