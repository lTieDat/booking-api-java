#!/usr/bin/env bash

set -euo pipefail

BASE_PACKAGE="${1:-com.bestpractice.api}"
MAIN_SRC="${2:-src/main/java}"
TEST_SRC="${3:-src/test/java}"

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
PACKAGE_PATH="${BASE_PACKAGE//./\/}"
MAIN_BASE_DIR="${ROOT_DIR}/${MAIN_SRC}/${PACKAGE_PATH}"
TEST_BASE_DIR="${ROOT_DIR}/${TEST_SRC}/${PACKAGE_PATH}"
RESOURCE_DIR="${ROOT_DIR}/src/main/resources"
TEST_RESOURCE_DIR="${ROOT_DIR}/src/test/resources"

declare -a MAIN_DIRS=(
  ""
  "features"
  "shared"
  "shared/config"
  "shared/database"
  "shared/error"
  "shared/exception"
  "shared/request"
  "shared/util"
)

mkdir -p "${MAIN_BASE_DIR}" "${TEST_BASE_DIR}" "${RESOURCE_DIR}" "${TEST_RESOURCE_DIR}"
mkdir -p "${RESOURCE_DIR}/db/migration"

for dir in "${MAIN_DIRS[@]}"; do
  target_dir="${MAIN_BASE_DIR}/${dir}"
  mkdir -p "${target_dir}"
  if [ -z "$(ls -A "${target_dir}")" ]; then
    touch "${target_dir}/.gitkeep"
  fi
done

APP_FILE="${MAIN_BASE_DIR}/Application.java"
TEST_FILE="${TEST_BASE_DIR}/ApplicationTests.java"
PROPERTIES_FILE="${RESOURCE_DIR}/application.properties"
MIGRATION_KEEP_FILE="${RESOURCE_DIR}/db/migration/.gitkeep"
TEST_PROPERTIES_FILE="${TEST_RESOURCE_DIR}/application-test.properties"

if [ ! -f "${APP_FILE}" ]; then
  cat <<EOF > "${APP_FILE}"
package ${BASE_PACKAGE};

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
EOF
fi

if [ ! -f "${TEST_FILE}" ]; then
  cat <<EOF > "${TEST_FILE}"
package ${BASE_PACKAGE};

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class ApplicationTests {

    @Test
    void contextLoads() {
    }
}
EOF
fi

if [ ! -f "${PROPERTIES_FILE}" ]; then
  APP_NAME="$(basename "${ROOT_DIR}")"
  DEFAULT_DB_NAME="${APP_NAME//-/_}"
  cat <<EOF > "${PROPERTIES_FILE}"
spring.application.name=${APP_NAME}

spring.datasource.url=\${DB_URL:jdbc:postgresql://localhost:5432/${DEFAULT_DB_NAME}}
spring.datasource.username=\${DB_USERNAME:postgres}
spring.datasource.password=\${DB_PASSWORD:postgres}
spring.datasource.driver-class-name=org.postgresql.Driver

spring.jpa.hibernate.ddl-auto=validate
spring.jpa.open-in-view=false
spring.jpa.properties.hibernate.jdbc.time_zone=UTC

spring.flyway.locations=classpath:db/migration
EOF
fi

if [ ! -f "${MIGRATION_KEEP_FILE}" ]; then
  touch "${MIGRATION_KEEP_FILE}"
fi

if [ ! -f "${TEST_PROPERTIES_FILE}" ]; then
  cat <<EOF > "${TEST_PROPERTIES_FILE}"
spring.datasource.url=jdbc:h2:mem:test_db;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE
spring.datasource.driver-class-name=org.h2.Driver
spring.datasource.username=sa
spring.datasource.password=

spring.jpa.hibernate.ddl-auto=validate
spring.jpa.open-in-view=false
spring.jpa.properties.hibernate.jdbc.time_zone=UTC

spring.flyway.locations=classpath:db/migration
EOF
fi

echo "Scaffolded package structure for ${BASE_PACKAGE}"
echo "Main source: ${MAIN_BASE_DIR}"
echo "Test source: ${TEST_BASE_DIR}"
