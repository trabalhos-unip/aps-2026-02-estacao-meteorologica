// Script PROVISÓRIO: mostra a última leitura vinda do Servlet /api/leituras/ultima.
fetch('/api/leituras/ultima')
  .then(resposta => resposta.json())
  .then(dados => { document.getElementById('ultima').textContent = JSON.stringify(dados, null, 2); })
  .catch(() => { document.getElementById('ultima').textContent = 'Não foi possível consultar o servidor.'; });
