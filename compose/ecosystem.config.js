// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

const path = require('path')

const instance = parseInt(process.env.EVAKA_INSTANCE || '0', 10)

const ports = {
  db: 5432 + instance,
  redis: 6379 + instance,
  s3: 9876 + instance,
  sftp: 2222 + instance,
  idp: 9090 + instance,
  apigw: 3000 + instance,
  service: 8888 + instance,
  frontend: 9099 + instance
}

const defaults = {
  autorestart: false
}

module.exports = {
  apps: [{
    name: 'apigw',
    script: 'yarn && yarn clean && yarn dev',
    cwd: path.resolve(__dirname, '../apigw'),
    env: {
      HTTP_PORT: ports.apigw,
      EVAKA_SERVICE_URL: `http://localhost:${ports.service}`,
      REDIS_PORT: ports.redis,
      EVAKA_BASE_URL: `http://localhost:${ports.frontend}`,
      SFI_SAML_CALLBACK_URL: `http://localhost:${ports.frontend}/api/application/auth/saml/login/callback`,
      SFI_SAML_ENTRYPOINT: `http://localhost:${ports.idp}/idp/sso`,
      SFI_SAML_LOGOUT_URL: `http://localhost:${ports.idp}/idp/slo`,
      SFI_SAML_ISSUER: `http://localhost:${ports.frontend}/api/application/auth/saml/`
    },
    ...defaults
  }, {
    name: 'frontend',
    script: 'yarn && yarn clean && yarn dev',
    cwd: path.resolve(__dirname, '../frontend'),
    env: {
      ICONS: process.env.ICONS,
      EVAKA_FRONTEND_PORT: ports.frontend,
      EVAKA_APIGW_PORT: ports.apigw
    },
    ...defaults
  }, {
    name: 'service',
    script: `${__dirname}/run-after-db.sh`,
    args: './gradlew --no-daemon bootRun',
    cwd: path.resolve(__dirname, '../service'),
    env: {
      SERVER_PORT: ports.service,
      EVAKA_DATABASE_URL: `jdbc:postgresql://localhost:${ports.db}/evaka_local`,
      EVAKA_LOCAL_S3_URL: `https://localhost:${ports.s3}`
    },
    ...defaults
  }, /*{
    name: 'ai',
    script: `${__dirname}/run-after-db.sh`,
    args: './gradlew --no-daemon bootRun',
    cwd: path.resolve(__dirname, '../../evaka-ai'),
    ...defaults
  }*/
  ],
}
