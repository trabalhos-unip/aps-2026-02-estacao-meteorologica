const API_BASE = "/api/leituras";

async function request(path, base = API_BASE) {
  // Leituras e previsões mudam enquanto a aplicação está aberta. Nunca use uma
  // resposta armazenada pelo navegador/proxy para uma atualização do painel.
  const response = await fetch(`${base}${path}`, {
    headers: { Accept: "application/json", "Cache-Control": "no-cache" },
    cache: "no-store",
  });
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
  buscarPrevisoes: (inicio, fim) => request(`/previsoes?inicio=${encodeURIComponent(inicio)}&fim=${encodeURIComponent(fim)}`, "/api/resumos-horarios"),
};

export const estacaoApi = {
  ativa: () => request("/api/estacao", ""),
  sugestoes: (termo, estado = "") => request(
    `/sugestoes?termo=${encodeURIComponent(termo)}&estado=${encodeURIComponent(estado)}`,
    "/api/estacao"
  ),
  selecionar: async (cidade, estado) => {
    const response = await fetch("/api/estacao", {
      method: "POST",
      headers: { "Content-Type": "application/json", Accept: "application/json", "Cache-Control": "no-cache" },
      cache: "no-store",
      body: JSON.stringify({ cidade, estado }),
    });
    const body = await response.json().catch(() => null);
    if (!response.ok) throw new Error(body?.erro || "Não foi possível alterar a estação.");
    return body;
  },
};
