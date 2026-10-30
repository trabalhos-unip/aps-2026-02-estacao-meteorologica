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
git pull --ff-only origin "$BRANCH"
"${COMPOSE[@]}" up -d --build --remove-orphans

# Caddy le o arquivo montado no inicio; reinicia para aplicar uma eventual
# atualizacao do Caddyfile e renovar a resolucao do webapp no Docker.
"${COMPOSE[@]}" restart caddy
"${COMPOSE[@]}" ps
