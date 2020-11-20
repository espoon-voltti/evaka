<!--
SPDX-FileCopyrightText: 2017-2020 City of Espoo

SPDX-License-Identifier: LGPL-2.1-or-later
-->

# evaka-message-service

This service handles messaging between [evaka-service](../service/README.md) and Suomi.fi Viestit.

This services exposes a REST API for sending Suomi.fi messages to citizens.

Suomi.fi Viestit WSDL API documentation: <https://palveluhallinta.suomi.fi/fi/tuki/artikkelit/5c71aeaf46e7410049a6d8ad>

## Prerequisites

- Latest java 11 with JAVA_HOME pointing to it
- Docker
- *For local testing against SFI*: access to Suomi.fi Viestit QAT [see below](#Local-testing-against-SFI)

## Development

### Build

Build `./gradlew build`

### Run

To start the dev server on port 8083:

```sh
./gradlew bootRun
```

### Tests

Run unit tests:

```sh
./gradlew test
```

Run integration tests:

```sh
./gradlew integrationTest
```

Run linter autofix:

```sh
./gradlew ktlintFormat
```

If you want to set ktlint formatter rules as your IDEA kotlin formatting rules, run:

```sh
./gradlew ktlintApplyToIdea
```

### Local testing against SFI

**NOTE:** These instructions are only relevant for Voltti developers as they require
an existing connection and configuration for Suomi.fi Viestit.

Use the `SfiApiTest` class to run the test, remember to remove the `@Ignore` from the class.

Add the following to your application-test.properties:

```ini
fi.espoo.evaka.msg.sfi.ws.trustStore.password=
fi.espoo.evaka.msg.sfi.ws.trustStore.location=

fi.espoo.evaka.msg.sfi.ws.keyStore.location=
fi.espoo.evaka.msg.sfi.ws.keyStore.password=
```

The values for the store passwords can be found from SSM:

```sh
/<env>/evaka-message-srv/sfi_truststore_password
/<env>/evaka-message-srv/sfi_keystore_password
```

And the stores themselves can be loaded from S3 from the deployment bucket, under `/message-srv`.

Connection to SFI APIS are allowed from the public IPs of test/staging/prod environments. It's not enough to
use just bastion since it routes outgoing traffic via a different IP. One machine that routes the requests correctly
are the ECS cluster nodes.

Update your ssh config:

```ssh-config
Host voltti-bastion-test
    HostName <BASTION HOST>
    User your.name.at.company.com
    IdentityFile ~/.ssh/id_rsa

Host sfi-tunnel-test
    HostName <ECS NODE>
    User your.name.at.company.com
    ProxyCommand ssh -W %h:%p voltti-bastion-test
    IdentityFile ~/.ssh/id_rsa
    Localforward 15267 192.49.232.194:443
```

Replace `<ECS NODE>` with an IP of the target environment's ECS node that
you can find with (it changes ~daily):

```sh
aws ec2 describe-instances \
    --filters "Name=tag:Name,Values=voltti-ecs-ec2-instance-test" \
    --output text \
    --query "Reservations[].Instances[].[NetworkInterfaces[0].PrivateIpAddress][0]"
```

The `LocalForward` target IP is for QAT, if you need to send to prod SFI, check the IP from [wiki](https://voltti.atlassian.net/wiki/spaces/EVAKA/pages/852328491/Suomi.fi+Viestit+k+ytt+notto)
from the VIA excel.

SSH to the instance you just specified above with `ssh sfi-tunnel-test` and run your test. If the test succeeds, you should be
able to read the message from the SFI test environment. The address and passwords are on the  [wiki](https://voltti.atlassian.net/wiki/spaces/EVAKA/pages/852328491/Suomi.fi+Viestit+k+ytt+notto)
page.

You can see the content of the replies and responses in the log because the trace logging is enabled in `application-test.properties`:

```ini
logging.level.org.springframework.security=debug
logging.level.org.springframework.ws.client.MessageTracing.sent=DEBUG
logging.level.org.springframework.ws.server.MessageTracing.sent=DEBUG
logging.level.org.springframework.ws.client.MessageTracing.received=TRACE
logging.level.org.springframework.ws.server.MessageTracing.received=TRACE
```

If you need to, you can change the recipient SSN in the test, the address does not have to match. The sanomaVarmenneNimi
in the `Viranomainen` object should also match the environment:

```kotlin
sanomaVarmenneNimi = "Evaka-staging-Suomi.fi_Viestit"
```

Should work for staging and test (both environments pipe to VIA QAT and are accepted there) unauthenticated environments.
It seems that when using authentication, this parameter is probably ignored and does not matter. You can check the common
name of the certificate from the [wiki](https://voltti.atlassian.net/wiki/spaces/EVAKA/pages/852328491/Suomi.fi+Viestit+k+ytt+notto)
 VIA excel or the keystores if you need to change them.

## Maintenance

### Key and trust stores

This service uses a key placed in its key store for Suomi.fi Viestit integration's
authentication, and trusts in the root CA certificate via its trust store.

The root CA certificates originate from [Valtori/Entrust](https://valtori.fi/yhteinen-integraatioalusta-via-julkiset-varmenteet).

Some useful commands for managing the trust store:

```sh
# Download current trust store from S3:
aws s3 cp s3://${DEPLOYMENT_BUCKET}/message-srv/trustStore.jks trustStore.jks

# Get trust store password:
aws ssm get-parameter --with-decryption --name /${TARGET_ENV}/evaka-message-srv/sfi_truststore_password --query 'Parameter.Value' --output text

# List certificates in trust store (prompts for the password, see above):
keytool -list -v -keystore trustStore.jks

# Rotate password of trust store:
keytool -storepasswd -keystore trustStore.jks
aws ssm put-parameter --name /${TARGET_ENV}/evaka-message-srv/sfi_truststore_password --value 'supersecretpassword' --type SecureString --overwrite
aws s3 cp trustStore.jks s3://${DEPLOYMENT_BUCKET}/message-srv/trustStore.jks
```

#### Updating certificates in trust store

Repeat for all environments:

1. Get new root CA certificate from [Valtori](https://valtori.fi/yhteinen-integraatioalusta-via-julkiset-varmenteet)
1. Download current trust store
1. Get password for trust store
1. Add new trusted CA cert (new cert in file `Root.crt`, filename doesn't matter):

    ```sh
    keytool -import -trustcacerts -keystore trustStore.jks -alias $NEW_ALIAS -file Root.crt
    ```

1. Remove old cert from trust store:

    ```sh
    keytool -delete -alias $OLD_ALIAS -keystore trustStore.jks
    ```

    - To find the old alias, list the keys with: `keytool -list -v -keystore trustStore.jks`
    - **NOTE:** Only delete when its no longer in use (i.e. you can add a new one beforehand but don't delete the old one until VIA has updated their certificate)
1. Upload updated trust store
1. Re-deploy all ECS service tasks:

    ```sh
    aws ecs update-service --force-new-deployment --service evaka-message-srv --cluster voltti-ecs-cluster-$ENV
    ```

    - **NOTE:** Cluster and service name are deployment specific
