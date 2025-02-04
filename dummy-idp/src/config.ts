// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

type EnvVariables = typeof envVariables
const envVariables = {
  HTTP_PORT: 9090,
  IDP_PUBLIC_CERT_PATH: 'test-cert/dummy-idp.pem',
  IDP_PRIVATE_KEY_PATH: 'test-cert/dummy-idp.key',
  SP_PUBLIC_CERT_PATH: 'test-cert/sp-public-cert.pem',
  SP_ENTITY_ID: 'http://localhost:9099/api/application/auth/saml/',
  SP_SSO_CALLBACK_URL:
    'http://localhost:9099/api/application/auth/saml/login/callback',
  SP_SLO_CALLBACK_URL:
    'http://localhost:9099/api/application/auth/saml/logout/callback'
}

type Parser<T> = (value: string) => T

const unchanged: Parser<string> = (value) => value

const parseInteger: Parser<number> = (value) => {
  const result = Number.parseInt(value, 10)
  if (Number.isNaN(result)) throw new Error('Invalid integer')
  return result
}

function parseEnv<K extends keyof EnvVariables>(
  key: K,
  f: Parser<NonNullable<EnvVariables[K]>>
): NonNullable<EnvVariables[K]> {
  const value = process.env[key]?.trim()
  try {
    return value == null || value === '' ? envVariables[key] : f(value)
  } catch (err) {
    const message = err instanceof Error ? err.message : String(err)
    throw new Error(`${message}: ${key}=${value}`)
  }
}

function configFromEnv(): EnvVariables {
  return {
    HTTP_PORT: parseEnv('HTTP_PORT', parseInteger),
    IDP_PUBLIC_CERT_PATH: parseEnv('IDP_PUBLIC_CERT_PATH', unchanged),
    IDP_PRIVATE_KEY_PATH: parseEnv('IDP_PRIVATE_KEY_PATH', unchanged),
    SP_PUBLIC_CERT_PATH: parseEnv('SP_PUBLIC_CERT_PATH', unchanged),
    SP_ENTITY_ID: parseEnv('SP_ENTITY_ID', unchanged),
    SP_SSO_CALLBACK_URL: parseEnv('SP_SSO_CALLBACK_URL', unchanged),
    SP_SLO_CALLBACK_URL: parseEnv('SP_SLO_CALLBACK_URL', unchanged)
  }
}

export const config = configFromEnv()
