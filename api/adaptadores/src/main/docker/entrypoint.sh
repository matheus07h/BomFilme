#!/bin/sh
set -eu

# Render fornece uma URI postgres://; o driver JDBC precisa do prefixo jdbc:postgresql://.
# Usuário e senha são passados em variáveis separadas pelo Blueprint.
case "${BOMFILME_DB_URL:-}" in
  postgres://*|postgresql://*)
    endereco="${BOMFILME_DB_URL#*://}"
    endereco="${endereco#*@}"
    export BOMFILME_DB_URL="jdbc:postgresql://${endereco}"
    ;;
esac

exec java --add-opens java.base/java.lang=ALL-UNNAMED -jar /aplicacao/quarkus-run.jar
