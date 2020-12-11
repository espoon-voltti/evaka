// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import certificates from './certificates'

export const gatewayRoles = ['enduser', 'internal'] as const
export type NodeEnv = typeof nodeEnvs[number]
export const nodeEnvs = ['local', 'test', 'production'] as const

function ifNodeEnv<T>(envs: NodeEnv[], value: T): T | undefined {
  return envs.some((env) => process.env.NODE_ENV === env) ? value : undefined
}

function required<T>(value: T | undefined): T {
  if (value === undefined) {
    throw new Error('Configuration parameter was not provided')
  }
  return value
}

function parseInteger(value: string): number {
  const result = Number.parseInt(value, 10)
  if (Number.isNaN(result)) throw new Error('Invalid integer')
  return result
}

const booleans: Record<string, boolean> = {
  1: true,
  0: false,
  true: true,
  false: false
}

function parseBoolean(value: string): boolean {
  if (value in booleans) return booleans[value]
  throw new Error('Invalid boolean')
}

function parseEnum<T extends string>(
  variants: readonly T[]
): (value: string) => T {
  return (value) => {
    for (const variant of variants) {
      if (value === variant) return variant
    }
    throw new Error(`Invalid enum (expected one of ${variants})`)
  }
}

function env<T>(key: string, parser: (value: string) => T): T | undefined {
  const value = process.env[key]
  if (value === undefined || value === '') return undefined
  try {
    return parser(value)
  } catch (err) {
    throw new Error(`${err.message}: ${key}=${value}`)
  }
}

function envArray<T>(
  key: string,
  parser: (value: string) => T,
  separator = ','
): T[] | undefined {
  const value = process.env[key]
  if (value === undefined || value === '') return undefined
  const values = value.split(separator)
  try {
    return values.map(parser)
  } catch (err) {
    throw new Error(`${err.message}: ${key}=${value}`)
  }
}

export const gatewayRole = env('GATEWAY_ROLE', parseEnum(gatewayRoles))
export const nodeEnv = env('NODE_ENV', parseEnum(nodeEnvs))
export const appBuild = process.env.APP_BUILD ?? 'UNDEFINED'
export const appCommit = process.env.APP_COMMIT ?? 'UNDEFINED'
export const hostIp = process.env.HOST_IP ?? 'UNDEFINED'
export const debug = ifNodeEnv(['local', 'test'], true) ?? false

export const jwtPrivateKey = required(
  process.env.JWT_PRIVATE_KEY ??
    ifNodeEnv(['local', 'test'], 'config/test-cert/jwt_private_key.pem')
)
export const jwtKid = process.env.JWT_KID ?? `evaka-${gatewayRole || 'dev'}-gw`

export const evakaServiceUrl = required(
  process.env.EVAKA_SERVICE_URL ??
    ifNodeEnv(['local', 'test'], 'http://localhost:8888')
)
// Google API requires an API key even in local development and we cannot restrict its usage to local environments
// so none is provided by default.
export const googleApiKey = process.env.GOOGLE_API_KEY
export const cookieSecret = required(
  process.env.COOKIE_SECRET ??
    ifNodeEnv(['local', 'test'], 'A very hush hush cookie secret.')
)
export const useSecureCookies =
  env('USE_SECURE_COOKIES', parseBoolean) ??
  ifNodeEnv(['local', 'test'], false) ??
  true

export const prettyLogs =
  env('PRETTY_LOGS', parseBoolean) ?? ifNodeEnv(['local'], true) ?? false

export const volttiEnv = process.env.VOLTTI_ENV ?? ifNodeEnv(['local'], 'local')

export const redisHost =
  process.env.REDIS_HOST ?? ifNodeEnv(['local'], 'localhost')

export const redisPort =
  env('REDIS_PORT', parseInteger) ?? ifNodeEnv(['local'], 6379)

export const redisPassword = process.env.REDIS_PASSWORD
export const redisTlsServerName = process.env.REDIS_TLS_SERVER_NAME
export const redisDisableSecurity =
  env('REDIS_DISABLE_SECURITY', parseBoolean) ??
  ifNodeEnv(['local'], true) ??
  false

export const httpPort = {
  enduser: env('HTTP_PORT', parseInteger) ?? 3010,
  internal: env('HTTP_PORT', parseInteger) ?? 3020
}
export const sessionTimeoutMinutes =
  env('SESSION_TIMEOUT_MINUTES', parseInteger) ?? 32

export const enableDevApi =
  env('ENABLE_DEV_API', parseBoolean) ??
  ifNodeEnv(['local', 'test'], true) ??
  false

const certificateNames = Object.keys(certificates) as ReadonlyArray<
  keyof typeof certificates
>

export const devLoginEnabled =
  env('DEV_LOGIN', parseBoolean) ?? ifNodeEnv(['local', 'test'], true) ?? false

export const eadSamlCallbackUrl = process.env.EAD_SAML_CALLBACK_URL
export const eadSamlIssuer = process.env.EAD_SAML_ISSUER
export const eadSamlPublicCert = envArray(
  'EAD_SAML_PUBLIC_CERT',
  parseEnum(certificateNames)
)
export const eadSamlPrivateCert = process.env.EAD_SAML_PRIVATE_CERT

export const sfiMock =
  env('SFI_MOCK', parseBoolean) ?? ifNodeEnv(['local', 'test'], true) ?? false
export const sfiSamlCallbackUrl = process.env.SFI_SAML_CALLBACK_URL
export const sfiSamlEntryPoint = process.env.SFI_SAML_ENTRY_POINT
export const sfiSamlLogoutUrl = process.env.SFI_SAML_LOGOUT_URL
export const sfiSamlIssuer = process.env.SFI_SAML_ISSUER
export const sfiSamlPublicCert = envArray(
  'SFI_SAML_PUBLIC_CERT',
  parseEnum(certificateNames)
)
export const sfiSamlPrivateCert = process.env.SFI_SAML_PRIVATE_CERT

export const evakaSamlCallbackUrl = required(
  process.env.EVAKA_SAML_CALLBACK_URL ??
    ifNodeEnv(
      ['local'],
      `http://localhost:9093/api/internal/auth/evaka/login/callback`
    )
)

export const evakaSamlEntrypoint = required(
  process.env.EVAKA_SAML_ENTRYPOINT ??
    ifNodeEnv(
      ['local'],
      'http://localhost:8080/auth/realms/evaka/protocol/saml'
    )
)
