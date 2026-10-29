export const iconSprite = `
<svg class="icon-sprite" xmlns="http://www.w3.org/2000/svg" aria-hidden="true">
  <symbol id="i-home" viewBox="0 0 24 24"><path d="m3 10 9-7 9 7v10h-6v-6H9v6H3z"/></symbol>
  <symbol id="i-history" viewBox="0 0 24 24"><path d="M3 11a9 9 0 1 1 2.3 7M3 4v7h7M12 7v5l4 2"/></symbol>
  <symbol id="i-flask" viewBox="0 0 24 24"><path d="M9 3h6M10 3v6l-6 10a2 2 0 0 0 2 3h12a2 2 0 0 0 2-3L14 9V3M8 16h8"/></symbol>
  <symbol id="i-settings" viewBox="0 0 24 24"><path d="M10 2h4l.6 2.3 2 1.1 2.3-.7 2 3.5-1.7 1.7v2.2l1.7 1.7-2 3.5-2.3-.7-2 1.1L14 22h-4l-.6-2.3-2-1.1-2.3.7-2-3.5 1.7-1.7v-2.2L3.1 10l2-3.5 2.3.7 2-1.1z"/><circle cx="12" cy="12" r="3"/></symbol>
  <symbol id="i-pin" viewBox="0 0 24 24"><path d="M19 10c0 5-7 11-7 11S5 15 5 10a7 7 0 1 1 14 0Z"/><circle cx="12" cy="10" r="2.2"/></symbol>
  <symbol id="i-search" viewBox="0 0 24 24"><circle cx="10.8" cy="10.8" r="6.4"/><path d="m16 16 5 5"/></symbol>
  <symbol id="i-chevron" viewBox="0 0 24 24"><path d="m6 9 6 6 6-6"/></symbol>
  <symbol id="i-calendar" viewBox="0 0 24 24"><rect x="3" y="5" width="18" height="16" rx="2"/><path d="M7 3v4m10-4v4M3 10h18"/></symbol>
  <symbol id="i-thermometer" viewBox="0 0 24 24"><path d="M10 14V5a2 2 0 1 1 4 0v9a5 5 0 1 1-4 0Z"/><path d="M12 8v9"/></symbol>
  <symbol id="i-drop" viewBox="0 0 24 24"><path d="M12 2C8 8 5 12 5 16a7 7 0 0 0 14 0c0-4-3-8-7-14Z"/></symbol>
  <symbol id="i-rain" viewBox="0 0 24 24"><path d="M5 15a4 4 0 0 1 .5-8 6 6 0 0 1 11-1 4.5 4.5 0 0 1 2.5 9H5Zm3 3-1 3m5-3-1 3m5-3-1 3"/></symbol>
  <symbol id="i-wind" viewBox="0 0 24 24"><path d="M2 8h12a3 3 0 1 0-3-3M2 12h17a2 2 0 1 1-2 2M2 17h9a3 3 0 1 1-3 3"/></symbol>
  <symbol id="i-clock" viewBox="0 0 24 24"><circle cx="12" cy="12" r="9"/><path d="M12 6v6l4 2"/></symbol>
  <symbol id="i-database" viewBox="0 0 24 24"><ellipse cx="12" cy="5" rx="8" ry="3"/><path d="M4 5v14c0 1.7 3.6 3 8 3s8-1.3 8-3V5M4 12c0 1.7 3.6 3 8 3s8-1.3 8-3"/></symbol>
  <symbol id="i-cloud" viewBox="0 0 24 24"><path d="M5 18a4 4 0 0 1 0-8 6 6 0 0 1 11-2 5 5 0 1 1 2 10H5Z"/></symbol>
  <symbol id="i-sun" viewBox="0 0 24 24"><circle cx="12" cy="12" r="4"/><path d="M12 1v3m0 16v3M1 12h3m16 0h3M4.2 4.2l2.1 2.1m11.4 11.4 2.1 2.1m0-15.6-2.1 2.1M6.3 17.7l-2.1 2.1"/></symbol>
  <symbol id="i-moon" viewBox="0 0 24 24"><path d="M21 13a9 9 0 1 1-10-10 7 7 0 0 0 10 10Z"/></symbol>
  <symbol id="i-monitor" viewBox="0 0 24 24"><rect x="3" y="4" width="18" height="12" rx="2"/><path d="M8 20h8m-4-4v4"/></symbol>
  <symbol id="i-chart" viewBox="0 0 24 24"><path d="M4 20V9m6 11V4m6 16v-8m4 8v-5M2 20h20"/></symbol>
  <symbol id="i-info" viewBox="0 0 24 24"><circle cx="12" cy="12" r="10"/><path d="M12 11v6m0-10h.01"/></symbol>
  <symbol id="i-play" viewBox="0 0 24 24"><path d="m8 4 12 8-12 8z"/></symbol>
  <symbol id="i-file" viewBox="0 0 24 24"><path d="M6 2h9l4 4v16H6zM15 2v5h4M9 12h7m-7 4h7"/></symbol>
  <symbol id="i-check" viewBox="0 0 24 24"><path d="m4 12 5 5L20 6"/></symbol>
  <symbol id="i-arrow" viewBox="0 0 24 24"><path d="M4 12h16m-6-6 6 6-6 6"/></symbol>
  <symbol id="i-chevron" viewBox="0 0 24 24"><path d="m9 18 6-6-6-6"/></symbol>
  <symbol id="i-code" viewBox="0 0 24 24"><path d="m8 7-5 5 5 5m8-10 5 5-5 5m-3-14-2 18"/></symbol>
  <symbol id="i-refresh" viewBox="0 0 24 24"><path d="M20 9a8 8 0 1 0 1 6M20 3v6h-6"/></symbol>
</svg>`;

export function icon(name, className = "") {
  return `<svg class="icon ${className}" aria-hidden="true"><use href="#i-${name}"></use></svg>`;
}

const menu = [
  { path: "/", label: "Visão geral", icon: "home" },
  { path: "/historico", label: "Histórico", icon: "history" },
  { path: "/simulacoes", label: "Simulações", icon: "flask" },
  { path: "/configuracoes", label: "Configurações", icon: "settings" },
];

const titles = {
  "/": ["Visão geral", "Acompanhe em tempo real as condições meteorológicas"],
  "/historico": ["Histórico e análises", "Visualize a evolução das variáveis meteorológicas"],
  "/simulacoes": ["Simulações", "Exemplo de cenários meteorológicos para testes do sistema"],
  "/configuracoes": ["Configurações", "Ajuste os parâmetros de coleta de dados e o comportamento do sistema."],
};

export function renderShell(path, content) {
  const [title, subtitle] = titles[path];
  document.title = `${title} · Meteora`;
  document.querySelector("#app").innerHTML = `
    ${iconSprite}
    <div class="app-layout">
      <aside class="sidebar">
        <a href="/" class="brand" aria-label="Meteora, visão geral">
          <span class="brand-mark" aria-hidden="true"><span class="brand-sun"></span><span class="brand-cloud"></span></span>
          <strong>Meteora</strong><span>Estação Meteorológica</span><small>APS 2026/2</small>
        </a>
        <nav class="primary-nav" aria-label="Navegação principal">
          ${menu.map(item => `<a href="${item.path}" class="nav-link ${item.path === path ? "active" : ""}" ${item.path === path ? 'aria-current="page"' : ""}>${icon(item.icon)}<span>${item.label}</span></a>`).join("")}
        </nav>
        <div class="sidebar-foot">
          <div class="collector-card" id="collector-card">
            <div class="collector-title"><span class="status-dot neutral"></span><strong id="collector-title">Coleta não verificada</strong></div>
            <span>Última leitura</span><strong id="sidebar-last-reading">Carregando…</strong>
            <span id="sidebar-collector-note">O status do processo depende do back-end.</span>
          </div>
          <small>UNIP · APS 2026/2</small>
        </div>
      </aside>
      <div class="workspace">
        <header class="topbar">
          <div class="page-heading"><h1>${title}</h1><p id="page-subtitle">${subtitle}</p></div>
          <div class="top-actions">
            <span class="mode-badge" id="mode-badge" hidden>Dados simulados</span>
            <button class="theme-toggle" type="button" id="theme-toggle" aria-label="Ativar tema escuro" title="Ativar tema escuro">${icon("moon")}</button>
            <button class="location-pill" type="button" data-open-location-modal title="Alterar cidade da estação">${icon("search")}<span id="location-label">Localização da estação</span>${icon("pin")}</button>
          </div>
        </header>
        <main id="conteudo" class="page-content">${content}</main>
      </div>
    </div>
    <div id="location-modal" class="location-modal" role="dialog" aria-modal="true" aria-labelledby="location-modal-title" hidden>
      <div class="location-modal-card">
        <div class="location-modal-heading"><div><small>LOCALIZAÇÃO DA ESTAÇÃO</small><h2 id="location-modal-title">Escolha uma cidade</h2><p>A UF e as coordenadas são identificadas automaticamente.</p></div><button type="button" class="location-modal-close" data-close-location-modal aria-label="Fechar">×</button></div>
        <label class="location-search-label" for="location-city">Buscar cidade brasileira</label>
        <select id="location-city" data-placeholder="Digite uma cidade, por exemplo Porto de Galinhas" aria-label="Buscar cidade brasileira"><option value=""></option></select>
        <div class="location-modal-section"><span>Cidades sugeridas</span><div class="location-shortcuts" role="list"><button type="button" data-location-city="Sorocaba" data-location-state="SP">Sorocaba, SP</button><button type="button" data-location-city="Campinas" data-location-state="SP">Campinas, SP</button><button type="button" data-location-city="Porto Alegre" data-location-state="RS">Porto Alegre, RS</button><button type="button" data-location-city="Porto de Galinhas" data-location-state="PE">Porto de Galinhas, PE</button></div></div>
        <div class="location-modal-section" id="location-recent-section" hidden><span>Usadas recentemente</span><div class="location-recent" id="location-recent"></div></div>
        <div class="location-modal-footer"><span>Selecione uma cidade para atualizar os dados.</span><div><button type="button" class="secondary-button" data-close-location-modal>Cancelar</button><button type="button" class="primary-button" id="location-confirm" disabled>Usar esta cidade</button></div></div>
      </div>
    </div>
    <div id="toast" class="toast" role="status" aria-live="polite" hidden></div>`;
}

export function text(selector, value) {
  const element = document.querySelector(selector);
  if (element) element.textContent = value;
}

export function number(value, digits = 1) {
  const parsed = Number(value);
  return value === null || value === undefined || !Number.isFinite(parsed)
    ? "—"
    : parsed.toLocaleString("pt-BR", { minimumFractionDigits: digits, maximumFractionDigits: digits });
}

export function unit(value, suffix, digits = 1) {
  const formatted = number(value, digits);
  return formatted === "—" ? formatted : `${formatted} ${suffix}`;
}

export function parseDate(value) {
  if (!value) return null;
  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? null : date;
}

export function dateTime(value) {
  const date = value instanceof Date ? value : parseDate(value);
  return date ? new Intl.DateTimeFormat("pt-BR", { dateStyle: "short", timeStyle: "medium" }).format(date) : "—";
}

export function time(value) {
  const date = value instanceof Date ? value : parseDate(value);
  return date ? new Intl.DateTimeFormat("pt-BR", { timeStyle: "medium" }).format(date) : "—";
}

export function dateInput(date) {
  const local = new Date(date.getTime() - date.getTimezoneOffset() * 60_000);
  return local.toISOString().slice(0, 10);
}

export function apiDate(date) {
  const local = new Date(date.getTime() - date.getTimezoneOffset() * 60_000);
  return local.toISOString().slice(0, 19);
}

export function anchorDate(latest) {
  const stamp = parseDate(latest?.dataHoraColeta);
  if (!stamp) return new Date();
  return Date.now() - stamp.getTime() > 24 * 60 * 60_000 ? stamp : new Date();
}

export function updateCollectorCard(latest) {
  const stamp = parseDate(latest?.dataHoraColeta);
  text("#sidebar-last-reading", stamp ? dateTime(stamp) : "Nenhuma leitura");
  text("#collector-title", stamp ? "Leituras disponíveis" : "Sem leituras");
  text("#sidebar-collector-note", "Atividade do coletor não monitorada.");
  const dot = document.querySelector(".collector-title .status-dot");
  if (dot) dot.className = `status-dot ${stamp ? "success" : "neutral"}`;
}

export function showToast(message) {
  const toast = document.querySelector("#toast");
  toast.textContent = message;
  toast.hidden = false;
  clearTimeout(showToast.timer);
  showToast.timer = setTimeout(() => { toast.hidden = true; }, 5500);
}

export function fillTable(selector, readings, columns, emptyMessage = "Nenhuma leitura encontrada neste período.") {
  const body = document.querySelector(selector);
  body.replaceChildren();
  if (!readings.length) {
    const row = body.insertRow();
    const cell = row.insertCell();
    cell.colSpan = columns.length;
    cell.className = "empty-cell";
    cell.textContent = emptyMessage;
    return;
  }
  for (const reading of readings) {
    const row = body.insertRow();
    for (const column of columns) {
      const cell = row.insertCell();
      const value = column.value(reading);
      cell.textContent = value;
      if (column.badge) cell.className = "table-badge";
    }
  }
}
