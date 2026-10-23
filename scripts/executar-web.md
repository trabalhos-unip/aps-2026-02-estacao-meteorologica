# Executar a aplicação Web (webapp)

Aplicação 2: lê as leituras do MySQL e as mostra no navegador (página + gráfico).

## Antes de começar

Mesmos pré-requisitos do coletor (veja [`iniciar-coletor.md`](iniciar-coletor.md)): JDK 21+, Maven, `.env` e MySQL rodando.
O webapp pode ser iniciado antes ou depois do coletor; os dois usam o mesmo banco.

## Pelo IntelliJ IDEA

1. No seletor de execução, escolha **Webapp (servidor web)** e clique em ▶.
2. Abra <http://localhost:8080> no navegador.

## Pelo terminal (na raiz do projeto)

```bash
mvn install -DskipTests
mvn -pl webapp spring-boot:run
```

Depois abra <http://localhost:8080>. Para parar: `Ctrl+C`.

## Endereços úteis

| Endereço | Resultado esperado |
|---|---|
| <http://localhost:8080> | página com a última leitura e o gráfico |
| <http://localhost:8080/api/leituras/ultima> | JSON da leitura mais recente (ou `"Nenhuma leitura registrada ainda"`, HTTP 404) |
| <http://localhost:8080/api/leituras?limite=20> | JSON com as 20 últimas leituras, em ordem cronológica |
| <http://localhost:8080/api/leituras/total> | `{"total": N}` |

Se aparecer `"Banco de dados indisponível no momento"` (HTTP 503), confira se o MySQL está rodando e o `.env`.

## Configuração opcional (no `.env`)

| Variável | Padrão | Uso |
|---|---|---|
| `SERVIDOR_PORTA` | `8080` | porta do servidor web |
