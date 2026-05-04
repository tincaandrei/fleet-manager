#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"

cd "${ROOT_DIR}"
./mvnw -Popenapi clean compile spring-boot:start
trap './mvnw -Popenapi spring-boot:stop' EXIT
./mvnw -Popenapi org.springdoc:springdoc-openapi-maven-plugin:generate
