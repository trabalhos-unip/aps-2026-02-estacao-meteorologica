// dashboard.js — busca os dados no back-end (Servlet /api/leituras) e preenche a página.
// PROVISÓRIO: mostra a última leitura em texto e entrega o histórico ao gráfico.

async function buscarJson(url) {
  const resposta = await fetch(url);
  const dados = await resposta.json();
  if (!resposta.ok) {
    throw new Error(dados.erro || 'Erro ao consultar o servidor');
  }
  return dados;
}

async function carregarPainel() {
  const ultima = document.getElementById('ultima');
  try {
    ultima.textContent = JSON.stringify(await buscarJson('/api/leituras/ultima'), null, 2);
  } catch (erro) {
    ultima.textContent = erro.message; // ex.: "Nenhuma leitura registrada ainda"
  }

  try {
    desenharGrafico(await buscarJson('/api/leituras?limite=100'));
  } catch (erro) {
    document.getElementById('grafico').textContent = erro.message;
  }
}

carregarPainel();
