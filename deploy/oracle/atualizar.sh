#!/usr/bin/env bash
# Executado pela chave exclusiva do GitHub Actions na VM Oracle.
set -Eeuo pipefail

readonly PROJETO="$HOME/aps-2026-02-estacao-meteorologica"
readonly BRANCH="feature/probabilidade-chuva-horaria"
readonly WEBAPP_IMAGE="ghcr.io/trabalhos-unip/meteora-webapp"
readonly GHCR_CREDENTIALS="$HOME/.config/meteora/ghcr-pull.env"
readonly COMPOSE=(
  docker compose
  --env-file deploy/oracle/.env.oracle
  -f deploy/oracle/docker-compose.yml
  -f deploy/oracle/docker-compose.micro.yml
  -f deploy/oracle/docker-compose.https.yml
)

cd "$PROJETO"
ANTES="$(git rev-parse HEAD)"
git pull --ff-only origin "$BRANCH"
DEPOIS="$(git rev-parse HEAD)"
ARQUIVOS_ALTERADOS="$(git diff --name-only "$ANTES" "$DEPOIS")"

# A chave do GitHub continua restrita a este script. Para uma execucao manual
# do workflow, aceita somente o marcador fixo abaixo; nunca um comando livre.
case "${SSH_ORIGINAL_COMMAND:-}" in
  "") FORCAR_WEBAPP=false ;;
  deploy-webapp) FORCAR_WEBAPP=true ;;
  *) echo "Comando de deploy nao permitido." >&2; exit 2 ;;
esac

tem_alteracao() {
  grep -Eq "$1" <<< "$ARQUIVOS_ALTERADOS"
}

ATUALIZAR_WEBAPP=false
ATUALIZAR_COLETOR=false
ATUALIZAR_INFRA=false
REINICIAR_CADDY=false

# O módulo compartilhado e o POM raiz são dependência dos dois JARs.
if tem_alteracao '^(compartilhado/|pom.xml$)'; then
  ATUALIZAR_WEBAPP=true
  ATUALIZAR_COLETOR=true
fi

if tem_alteracao '^(webapp/|deploy/oracle/Dockerfile.webapp$)'; then
  ATUALIZAR_WEBAPP=true
fi

if [[ "$FORCAR_WEBAPP" == true ]]; then
  ATUALIZAR_WEBAPP=true
fi

if tem_alteracao '^(collector/|deploy/oracle/Dockerfile.collector$)'; then
  ATUALIZAR_COLETOR=true
fi

if tem_alteracao '^deploy/oracle/docker-compose.*\.yml$'; then
  ATUALIZAR_INFRA=true
fi

if tem_alteracao '^deploy/oracle/Caddyfile$'; then
  ATUALIZAR_INFRA=true
  REINICIAR_CADDY=true
fi

if [[ "$ATUALIZAR_WEBAPP" == true ]]; then
  # A imagem foi compilada no runner do GitHub. Se o pull falhar, o comando
  # encerra sem recriar o contêiner atual, preservando a versão em produção.
  echo "Baixando a imagem pronta da webapp: ${DEPOIS:0:12}"
  if [[ ! -r "$GHCR_CREDENTIALS" ]]; then
    echo "Credencial privada do GHCR ausente em $GHCR_CREDENTIALS." >&2
    exit 1
  fi
  # Arquivo local da VM, com permissao 600, nunca versionado nem carregado
  # como variavel de ambiente dos contêineres da aplicacao.
  source "$GHCR_CREDENTIALS"
  : "${GHCR_USERNAME:?GHCR_USERNAME ausente}"
  : "${GHCR_READ_TOKEN:?GHCR_READ_TOKEN ausente}"
  printf '%s' "$GHCR_READ_TOKEN" | docker login ghcr.io -u "$GHCR_USERNAME" --password-stdin >/dev/null
  trap 'docker logout ghcr.io >/dev/null 2>&1 || true' EXIT
  docker pull "$WEBAPP_IMAGE:$DEPOIS"
  docker tag "$WEBAPP_IMAGE:$DEPOIS" oracle-webapp:latest
  "${COMPOSE[@]}" up -d --no-build --no-deps webapp
fi

if [[ "$ATUALIZAR_COLETOR" == true ]]; then
  echo "Construindo apenas: collector"
  "${COMPOSE[@]}" build collector
  "${COMPOSE[@]}" up -d --no-deps collector
fi

if [[ "$ATUALIZAR_WEBAPP" == false && "$ATUALIZAR_COLETOR" == false ]]; then
  echo "Nenhuma alteracao de codigo Java/front-end para reconstruir."
fi

if [[ "$ATUALIZAR_INFRA" == true ]]; then
  echo "Aplicando configuracao de infraestrutura alterada."
  "${COMPOSE[@]}" up -d --remove-orphans
fi

if [[ "$REINICIAR_CADDY" == true ]]; then
  "${COMPOSE[@]}" restart caddy
fi
"${COMPOSE[@]}" ps
