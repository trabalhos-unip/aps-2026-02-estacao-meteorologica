#!/usr/bin/env bash
# Executado pela chave exclusiva do GitHub Actions na VM Oracle.
set -Eeuo pipefail

readonly PROJETO="$HOME/aps-2026-02-estacao-meteorologica"
readonly BRANCH="feature/probabilidade-chuva-horaria"
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

SERVICOS=()
if [[ "$ATUALIZAR_WEBAPP" == true ]]; then SERVICOS+=(webapp); fi
if [[ "$ATUALIZAR_COLETOR" == true ]]; then SERVICOS+=(collector); fi

if ((${#SERVICOS[@]})); then
  echo "Construindo apenas: ${SERVICOS[*]}"
  "${COMPOSE[@]}" build "${SERVICOS[@]}"
  "${COMPOSE[@]}" up -d --no-deps "${SERVICOS[@]}"
else
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
