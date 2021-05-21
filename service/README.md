<!--
SPDX-FileCopyrightText: 2017-2020 City of Espoo

SPDX-License-Identifier: LGPL-2.1-or-later
-->

# Evaka service

## Build and test

Prequisites

- Latest java 11 with JAVA_HOME pointing to it
- Docker

To build `./gradlew build`

## Run

### Start dependencies

Start / setup dependencies with [compose](../compose/README.md):

```sh
docker-compose up db sqs
```

### Start evaka-service

To start the dev server on port 8888:

```sh
./gradlew bootRun
```

To enable hot-swapping run the following command in a separate terminal in addition to the `bootRun` command:

```sh
./gradlew assemble --continuous
```

## Development

### Tests

Run unit tests:

```sh
./gradlew test
```

Run integration tests (needs to have docker daemon running):

```sh
./gradlew integrationTest
```

To start dev server connected to integration test database (suitable for e.g. running E2E tests without cluttering
development data):

```sh
./gradlew bootRunTest
```

Enable running flyway commands against integration test database in `gradle.properties`

Run linter autofix:

```sh
./gradlew formatKotlin
```

If you want to set ktlint formatter rules as your IDEA kotlin formatting rules, run:

```sh
./gradlew ktlintApplyToIdea
```

## Dev API

The service has a dev API in local, dev and test environments.
The API is used for creating test fixtures for E2E tests.
The API is defined in [DevApi.kt](src/main/kotlin/fi/espoo/evaka/shared/dev/DevApi.kt).

## Debugging integration tests

At the time being, we use TestContainers for integration tests, so the database lies in a
docker container while the tests are executed. You can access the db with eg.

```sh
docker exec -it <container> psql -U evaka_it -d evaka_it
```

where `<container>` is to be found with `docker ps`.

## Varda

Documentation: <https://backend-qa.varda-db.csc.fi/> (You need Voltti, Gofore or Reaktor IP)

### To test against Varda QA server:

- change application-dev.properties file: `fi.espoo.integration.varda.url=https://backend-qa.varda-db.csc.fi/api`
- get apiKey from <https://backend-qa.varda-db.csc.fi/varda/swagger/>
  - login using Opintopolku: See #evaka-tech pinned items
  - change `vardaService.getApiKey()` to return the apiKey

## Tips

### DB migration bookkeeping

Add `./service/list-migrations.sh` to your _existing_ pre-commit hook (`.git/hooks/pre-commit`) to enable automatic DB migration bookkeeping

OR if you don't have a pre-commit hook yet, you can create a new one:

```sh
cat >.git/hooks/pre-commit <<EOF
#!/bin/sh
./service/list-migrations.sh
EOF
chmod a+x .git/hooks/pre-commit
```

## OWASP dependency check

Service dependencies are checked for security vulnerabilities with
the [OWASP dependecy-check-gradle](https://jeremylong.github.io/DependencyCheck/dependency-check-gradle/index.html)
plugin. Dependencies are checked on every build with the command `./gradlew dependencyCheckAnalyze`. By default even
minor vulnerabilities break the build, but they can
be [suppressed](https://jeremylong.github.io/DependencyCheck/general/suppression.html) when needed. The suppression
rules are configured [here](./owasp-suppressions.xml).

### Add metadata to log entries

Use the [KLogger extensions in service-lib](../service-lib/src/main/kotlin/fi/espoo/voltti/logging/loggers/AppMiscLoggers.kt)
to add custom metadata to log entries (useful for querying/statistical analysis):

```kotlin
import fi.espoo.voltti.logging.loggers.error

try {
  logger.debug(mapOf("url" to url)) { "Doing something" }
  something()
} catch (error: FuelError) {
  val meta = mapOf(
      "method" to request.method,
      "url" to request.url,
      "body" to request.body.asString("application/json"),
      "errorMessage" to error.errorData.decodeToString()
  )
  logger.error(error, meta) { "Request failed, status ${error.response.statusCode}" }
}
```
