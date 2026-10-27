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
