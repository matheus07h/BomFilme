#!/bin/sh
set -eu

# Aceita URL JDBC diretamente ou converte uma URI PostgreSQL para o driver JDBC.
# Usuário e senha são passados em variáveis separadas pela plataforma.
case "${BOMFILME_DB_URL:-}" in
  postgres://*|postgresql://*)
    endereco="${BOMFILME_DB_URL#*://}"
    endereco="${endereco#*@}"
    export BOMFILME_DB_URL="jdbc:postgresql://${endereco}"
    ;;
esac

exec java --add-opens java.base/java.lang=ALL-UNNAMED -jar /aplicacao/quarkus-run.jar
