// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

const path = require('path')

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

// Externally reachable HTTPS URL set by `mise tunnel` (compose/tunnel.sh) for
// testing on external devices. When set, the SAML flow, the WebAuthn relying
// party and the app's own base URL all use it instead of localhost. The dummy IdP
// is reached through the frontend dev server's /idp proxy (frontend/vite.config.ts).
const tunnelUrl = process.env.TUNNEL_URL
const frontendBaseUrl = tunnelUrl || `http://localhost:${ports.frontend}`
const idpBaseUrl = tunnelUrl
  ? `${tunnelUrl}/idp`
  : `http://localhost:${ports.idp}/idp`
const webAuthnRpId = tunnelUrl ? new URL(tunnelUrl).hostname : 'localhost'

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
      SFI_SAML_ISSUER: `${frontendBaseUrl}/api/application/auth/saml/`
    },
    ...defaults
  }, {
    name: 'frontend',
    script: 'yarn && yarn clean && yarn dev',
    cwd: path.resolve(__dirname, '../frontend'),
    env: {
      ICONS: process.env.ICONS,
      EVAKA_FRONTEND_PORT: ports.frontend,
      EVAKA_APIGW_PORT: ports.apigw,
      EVAKA_IDP_PORT: ports.idp,
      EVAKA_CUSTOMIZATIONS: process.env.EVAKA_CUSTOMIZATIONS,
      TUNNEL_URL: tunnelUrl
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
      EVAKA_LOCAL_S3_URL: `https://localhost:${ports.s3}`,
      EVAKA_MUNICIPALITY: process.env.EVAKA_MUNICIPALITY,
      EVAKA_INTEGRATION_VTJ_MOCK_URL: `http://localhost:${ports.idp}`,
      EVAKA_WEBAUTHN_ORIGIN: frontendBaseUrl,
      EVAKA_WEBAUTHN_RP_ID: webAuthnRpId,
      EVAKA_FRONTEND_BASE_URL_FI: frontendBaseUrl,
      EVAKA_FRONTEND_BASE_URL_SV: frontendBaseUrl,
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
