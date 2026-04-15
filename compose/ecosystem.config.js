// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

const fs = require('fs')
const path = require('path')

function readOpenAIKey() {
  try {
    return fs.readFileSync(path.resolve(__dirname, '../.env.openai'), 'utf8').trim()
  } catch {
    return undefined
  }
}

const ports = {
  db: parseInt(process.env.EVAKA_DB_PORT || '5432', 10),
  redis: parseInt(process.env.EVAKA_REDIS_PORT || '6379', 10),
  s3: parseInt(process.env.EVAKA_S3_PORT || '9876', 10),
  sftp: parseInt(process.env.EVAKA_SFTP_PORT || '2222', 10),
  idp: parseInt(process.env.EVAKA_IDP_PORT || '9090', 10),
  apigw: parseInt(process.env.EVAKA_APIGW_PORT || '3000', 10),
  service: parseInt(process.env.SERVER_PORT || '8888', 10),
  frontend: parseInt(process.env.EVAKA_FRONTEND_PORT || '9099', 10)
}

// Optional: externally-reachable URL (e.g. cloudflared tunnel) for testing on
// real mobile devices. When set, the frontend must also proxy /idp to localhost:9090
// (see frontend/vite.config.ts).
const tunnelUrl = process.env.TUNNEL_URL
const frontendBaseUrl = tunnelUrl || `http://localhost:${ports.frontend}`
const idpBaseUrl = tunnelUrl ? `${tunnelUrl}/idp` : `http://localhost:${ports.idp}/idp`

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
      EVAKA_BASE_URL: frontendBaseUrl,
      SFI_SAML_CALLBACK_URL: `${frontendBaseUrl}/api/application/auth/saml/login/callback`,
      SFI_SAML_ENTRYPOINT: `${idpBaseUrl}/sso`,
      SFI_SAML_LOGOUT_URL: `${idpBaseUrl}/slo`,
      SFI_SAML_ISSUER: `${frontendBaseUrl}/api/application/auth/saml/`,
      OPENAI_API_KEY: readOpenAIKey(),
      PASSKEY_RP_NAME: 'eVaka'
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
