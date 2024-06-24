<!--
SPDX-FileCopyrightText: 2017-2020 City of Espoo

SPDX-License-Identifier: LGPL-2.1-or-later
-->

# Evaka service

## Build and test

Prerequisites

- Latest java 11 with JAVA_HOME pointing to it
- Docker

To build `./gradlew build`

## Run

### Start dependencies

Start / setup dependencies with [compose](../compose/README.md):

```sh
docker compose up db sqs
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
./gradlew ktlintFormat
```

If you use IntelliJ IDEA, it's recommended to install and enable the `ktlint` plugin.

## Dev API

The service has a dev API in local, dev and test environments.
The API is used for creating test fixtures for E2E tests.
The API is defined in [DevApi.kt](src/main/kotlin/fi/espoo/evaka/shared/dev/DevApi.kt).

## Debugging integration tests

The database lies in a docker container while the tests are executed. You can
access the db with eg.

```sh
docker exec -it <container> psql -U evaka_application_local -d evaka_it
```

where `<container>` is to be found with `docker ps`.

## Varda

Documentation: <https://backend-qa.varda-db.csc.fi/> (You need Voltti, Gofore or Reaktor IP)

### To test against Varda QA server

- change application-dev.properties file: `fi.espoo.integration.varda.url=https://backend-qa.varda-db.csc.fi/api`
- get apiKey from <https://backend-qa.varda-db.csc.fi/varda/swagger/>
  - login using Opintopolku: See #evaka-tech pinned items
  - change `vardaService.getApiKey()` to return the apiKey

## Maintenance

### Key and trust stores

This service uses a trust store to explicitly trust certificates of its
integrations, such as:

- X-Road Security Server's proxy (VTJ integration)
- [Varda](https://github.com/espoon-voltti/evaka/wiki/Varda-integraatio)
- [Koski](https://github.com/espoon-voltti/evaka/wiki/Koski-integraatio)

The trust store is configurable with:

- `FI_ESPOO_VOLTTI_VTJ_XROAD_TRUSTSTORE_LOCATION`: file location
- `FI_ESPOO_VOLTTI_VTJ_XROAD_TRUSTSTORE_TYPE`: trust store type (usually: "JKS")
- `FI_ESPOO_VOLTTI_VTJ_XROAD_TRUSTSTORE_PASSWORD`: trust store's password

and the actual store should contain whatever certificates you trust.

For authenticating in X-Road, the service also has a key store, configured with:

- `FI_ESPOO_VOLTTI_VTJ_XROAD_KEYSTORE_LOCATION`: file location
- `FI_ESPOO_VOLTTI_VTJ_XROAD_KEYSTORE_PASSWORD`: key store's password

and the actual store should contain your keys.

Some useful commands for managing the stores (same actions apply for both):

```sh
# Download current trust store from S3:
aws s3 cp s3://${DEPLOYMENT_BUCKET}/evaka-srv/trustStore.jks trustStore.jks

# Get trust store password from Parameter Store:
aws ssm get-parameter --with-decryption --name $PARAMETER_NAME --query 'Parameter.Value' --output text

# List certificates in trust store:
keytool -list -v -keystore trustStore.jks -storepass "$(aws ssm get-parameter --with-decryption --name $PARAMETER_NAME --query 'Parameter.Value' --output text)"

# Rotate password of trust store (prompts for new password):
keytool -storepasswd -keystore trustStore.jks -storepass "$(aws ssm get-parameter --with-decryption --name $PARAMETER_NAME --query 'Parameter.Value' --output text)"
aws ssm put-parameter --name $PARAMETER_NAME --value 'supersecretpassword' --type SecureString --overwrite
aws s3 cp trustStore.jks s3://${DEPLOYMENT_BUCKET}/evaka-srv/trustStore.jks
```

#### Updating certificates in trust store

Repeat for all environments:

1. Download current trust store:

    ```sh
    aws s3 cp s3://${DEPLOYMENT_BUCKET}/evaka-srv/trustStore.jks trustStore.jks
    ```

1. Find the old alias of the entry:

    ```sh
    keytool -list -v -keystore trustStore.jks -storepass "$(aws ssm get-parameter --with-decryption --name $PARAMETER_NAME --query 'Parameter.Value' --output text)"
    ```

1. *IF REPLACING:* Remove old cert from trust store:

    ```sh
    keytool -delete -alias $OLD_ALIAS -keystore trustStore.jks -storepass "$(aws ssm get-parameter --with-decryption --name $PARAMETER_NAME --query 'Parameter.Value' --output text)"
    ```

1. Fetch and import a new trusted cert:

    ```sh
    # Replace HOST with e.g. koski.opintopolku.fi to fetch the Koski production cert
    keytool -printcert -sslserver "${HOST}:443" -rfc | keytool -import -noprompt -alias $NEW_ALIAS -keystore trustStore.jks -storepass "$(aws ssm get-parameter --with-decryption --name $PARAMETER_NAME --query 'Parameter.Value' --output text)"
    ```

1. Upload updated trust store:

    ```sh
    aws s3 cp trustStore.jks s3://${DEPLOYMENT_BUCKET}/evaka-srv/trustStore.jks
    ```

1. Re-deploy all ECS service tasks:

    ```sh
    aws ecs update-service --force-new-deployment --service evaka-srv --cluster $CLUSTER_NAME
    ```

    - **NOTE:** Cluster and service name are deployment specific

### Generating VAPID private key for Web Push

1. Run `./gradlew generateVapidKey`
2. Copy the printed private key to configuration (environment variable name is EVAKA_WEB_PUSH_VAPID_PRIVATE_KEY)

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
the [OWASP dependency-check-gradle](https://jeremylong.github.io/DependencyCheck/dependency-check-gradle/index.html)
plugin. Dependencies are checked on every build with the command `./gradlew dependencyCheckAnalyze`. By default even
minor vulnerabilities break the build, but they can
be [suppressed](https://jeremylong.github.io/DependencyCheck/general/suppression.html) when needed. The suppression
rules are configured [here](./owasp-suppressions.xml).

### Add metadata to log entries

Use the [KLogger extensions in service-lib](service-lib/src/main/kotlin/fi/espoo/voltti/logging/loggers/AppMiscLoggers.kt)
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
