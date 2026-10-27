const API_BASE = "/api/leituras";

async function request(path, base = API_BASE) {
  const response = await fetch(`${base}${path}`, { headers: { Accept: "application/json" } });
  const body = await response.json().catch(() => null);
  if (!response.ok) throw new Error(body?.erro || "Não foi possível consultar os dados meteorológicos.");
  return body;
}

export const leiturasApi = {
  buscarUltima: () => request("/ultima"),
  buscarUltimas: (limite = 100) => request(`?limite=${encodeURIComponent(limite)}`),
  buscarPorPeriodo: (inicio, fim) => request(`/periodo?inicio=${encodeURIComponent(inicio)}&fim=${encodeURIComponent(fim)}`),
  buscarTotal: () => request("/total"),
  buscarFonte: () => request("/fonte", "/api"),
};

export const resumosDiariosApi = {
  buscarHistorico: (inicio, fim) => request(`/historico?inicio=${encodeURIComponent(inicio)}&fim=${encodeURIComponent(fim)}`, "/api/resumos-diarios"),
  buscarPrevisoes: (dias = 5) => request(`/previsoes?dias=${encodeURIComponent(dias)}`, "/api/resumos-diarios"),
};

export const resumosHorariosApi = {
  buscarHistorico: (inicio, fim) => request(`/historico?inicio=${encodeURIComponent(inicio)}&fim=${encodeURIComponent(fim)}`, "/api/resumos-horarios"),
};

export const estacaoApi = {
  ativa: () => request("/api/estacao", ""),
  selecionar: async (cidade, estado) => {
    const response = await fetch("/api/estacao", { method: "POST", headers: { "Content-Type": "application/json", Accept: "application/json" }, body: JSON.stringify({ cidade, estado }) });
    const body = await response.json().catch(() => null);
    if (!response.ok) throw new Error(body?.erro || "Não foi possível alterar a estação.");
    return body;
  },
};
