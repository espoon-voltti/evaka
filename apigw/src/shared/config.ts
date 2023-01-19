// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import certificates, { TrustedCertificates } from './certificates'
import type redis from 'redis'

export interface Config {
  ad: {
    mock: boolean
    externalIdPrefix: string
    userIdKey: string
    nameIdFormat: string
    decryptAssertions: boolean
    saml: EvakaSamlConfig | undefined
  }
  sfi: {
    mock: boolean
    saml: EvakaSamlConfig | undefined
  }
  redis: {
    host: string | undefined
    port: number | undefined
    password: string | undefined
    tlsServerName: string | undefined
    disableSecurity: boolean
  }
}

export const toRedisClientOpts = (config: Config): redis.ClientOpts => ({
  host: config.redis.host,
  port: config.redis.port,
  ...(!config.redis.disableSecurity && {
    tls: { servername: config.redis.tlsServerName },
    password: config.redis.password
  })
})

export interface EvakaSamlConfig {
  callbackUrl: string
  entryPoint: string
  logoutUrl: string
  issuer: string
  publicCert: string | TrustedCertificates[]
  privateCert: string
  validateInResponseTo: boolean
}

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
    const message = err instanceof Error ? err.message : String(err)
    throw new Error(`${message}: ${key}=${value}`)
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
    const message = err instanceof Error ? err.message : String(err)
    throw new Error(`${message}: ${key}=${value}`)
  }
}

export function configFromEnv(): Config {
  const adMock =
    env('AD_MOCK', parseBoolean) ??
    env('DEV_LOGIN', parseBoolean) ??
    ifNodeEnv(['local', 'test'], true) ??
    false
  const adCallbackUrl = process.env.AD_SAML_CALLBACK_URL
  const defaultUserIdKey =
    'http://schemas.microsoft.com/identity/claims/objectidentifier'
  const defaultNameIdFormat =
    'urn:oasis:names:tc:SAML:2.0:nameid-format:transient'
  const ad: Config['ad'] = {
    mock: adMock,
    externalIdPrefix: process.env.AD_SAML_EXTERNAL_ID_PREFIX ?? 'espoo-ad',
    userIdKey: process.env.AD_USER_ID_KEY ?? defaultUserIdKey,
    nameIdFormat: process.env.AD_NAME_ID_FORMAT ?? defaultNameIdFormat,
    decryptAssertions: env('AD_DECRYPT_ASSERTIONS', parseBoolean) ?? false,
    saml:
      adCallbackUrl && !adMock
        ? {
            callbackUrl: required(adCallbackUrl),
            entryPoint: required(process.env.AD_SAML_ENTRYPOINT_URL),
            logoutUrl: required(process.env.AD_SAML_LOGOUT_URL),
            issuer: required(process.env.AD_SAML_ISSUER),
            publicCert: required(
              envArray('AD_SAML_PUBLIC_CERT', parseEnum(certificateNames))
            ),
            privateCert: required(process.env.AD_SAML_PRIVATE_CERT),
            validateInResponseTo: true
          }
        : undefined
  }

  const sfiMock =
    env('SFI_MOCK', parseBoolean) ?? ifNodeEnv(['local', 'test'], true) ?? false
  const sfiCallbackUrl = process.env.SFI_SAML_CALLBACK_URL
  const sfi: Config['sfi'] = {
    mock: sfiMock,
    saml:
      sfiCallbackUrl && !sfiMock
        ? {
            callbackUrl: required(sfiCallbackUrl),
            entryPoint: required(process.env.SFI_SAML_ENTRYPOINT),
            logoutUrl: required(process.env.SFI_SAML_LOGOUT_URL),
            issuer: required(process.env.SFI_SAML_ISSUER),
            publicCert: required(
              envArray('SFI_SAML_PUBLIC_CERT', parseEnum(certificateNames))
            ),
            privateCert: required(process.env.SFI_SAML_PRIVATE_CERT),
            validateInResponseTo: true
          }
        : undefined
  }

  return {
    ad,
    sfi,
    redis: {
      host: process.env.REDIS_HOST ?? ifNodeEnv(['local'], 'localhost'),
      port: env('REDIS_PORT', parseInteger) ?? ifNodeEnv(['local'], 6379),
      password: process.env.REDIS_PASSWORD,
      tlsServerName: process.env.REDIS_TLS_SERVER_NAME,
      disableSecurity:
        env('REDIS_DISABLE_SECURITY', parseBoolean) ??
        ifNodeEnv(['local'], true) ??
        false
    }
  }
}

export const gatewayRole = env('GATEWAY_ROLE', parseEnum(gatewayRoles))
export const nodeEnv = env('NODE_ENV', parseEnum(nodeEnvs))
export const appBuild = process.env.APP_BUILD ?? 'UNDEFINED'
export const appCommit = process.env.APP_COMMIT ?? 'UNDEFINED'
export const hostIp = process.env.HOST_IP ?? 'UNDEFINED'
export const debug = ifNodeEnv(['local', 'test'], true) ?? false

export const tracingEnabled = process.env.DD_TRACE_ENABLED === 'true' ?? false
export const traceAgentHostname =
  process.env.DD_TRACE_AGENT_HOSTNAME ?? 'localhost'
export const traceAgentPort = env('DD_TRACE_AGENT_PORT', parseInteger) ?? 8126

export const jwtPrivateKey = required(
  process.env.JWT_PRIVATE_KEY ??
    ifNodeEnv(['local', 'test'], 'config/test-cert/jwt_private_key.pem')
)

export const serviceName = `evaka-${gatewayRole || 'dev'}-gw`
export const jwtKid = process.env.JWT_KID ?? serviceName

export const evakaBaseUrl = required(
  process.env.EVAKA_BASE_URL ?? ifNodeEnv(['local', 'test'], 'local')
)

export const evakaServiceUrl = required(
  process.env.EVAKA_SERVICE_URL ??
    ifNodeEnv(['local', 'test'], 'http://localhost:8888')
)
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

export const httpPort = {
  enduser: env('HTTP_PORT', parseInteger) ?? 3010,
  internal: env('HTTP_PORT', parseInteger) ?? 3020
}
export const sessionTimeoutMinutes =
  env('SESSION_TIMEOUT_MINUTES', parseInteger) ?? 32

export const pinSessionTimeoutSeconds =
  env('PIN_SESSION_TIMEOUT_SECONDS', parseInteger) ?? 10 * 60

export const enableDevApi =
  env('ENABLE_DEV_API', parseBoolean) ??
  ifNodeEnv(['local', 'test'], true) ??
  false

const certificateNames = Object.keys(
  certificates
) as ReadonlyArray<TrustedCertificates>

const evakaCallbackUrl =
  process.env.EVAKA_SAML_CALLBACK_URL ??
  ifNodeEnv(
    ['local', 'test'],
    `http://localhost:9099/api/internal/auth/evaka/login/callback`
  )

const evakaCustomerCallbackUrl =
  process.env.EVAKA_CUSTOMER_SAML_CALLBACK_URL ??
  ifNodeEnv(
    ['local', 'test'],
    `http://localhost:9099/api/application/auth/evaka-customer/login/callback`
  )

export const evakaSamlConfig: EvakaSamlConfig | undefined = evakaCallbackUrl
  ? {
      callbackUrl: required(evakaCallbackUrl),
      entryPoint: required(
        process.env.EVAKA_SAML_ENTRYPOINT ??
          ifNodeEnv(
            ['local', 'test'],
            'http://localhost:8080/auth/realms/evaka/protocol/saml'
          )
      ),
      // NOTE: Same as entrypoint, on purpose
      logoutUrl: required(
        process.env.EVAKA_SAML_ENTRYPOINT ??
          ifNodeEnv(
            ['local', 'test'],
            'http://localhost:8080/auth/realms/evaka/protocol/saml'
          )
      ),
      issuer: required(
        process.env.EVAKA_SAML_ISSUER ?? ifNodeEnv(['local', 'test'], 'evaka')
      ),
      publicCert: required(
        process.env.EVAKA_SAML_PUBLIC_CERT ??
          ifNodeEnv(['local', 'test'], 'config/test-cert/keycloak-local.pem')
      ),
      privateCert: required(
        process.env.EVAKA_SAML_PRIVATE_CERT ??
          ifNodeEnv(['local', 'test'], 'config/test-cert/saml-private.pem')
      ),
      validateInResponseTo: true
    }
  : undefined

export const evakaCustomerSamlConfig: EvakaSamlConfig | undefined =
  evakaCustomerCallbackUrl
    ? {
        callbackUrl: required(evakaCustomerCallbackUrl),
        entryPoint: required(
          process.env.EVAKA_CUSTOMER_SAML_ENTRYPOINT ??
            ifNodeEnv(
              ['local', 'test'],
              'http://localhost:8080/auth/realms/evaka-customer/protocol/saml'
            )
        ),
        logoutUrl: required(
          process.env.EVAKA_CUSTOMER_SAML_ENTRYPOINT ??
            ifNodeEnv(
              ['local', 'test'],
              'http://localhost:8080/auth/realms/evaka-customer/protocol/saml'
            )
        ),
        issuer: required(
          process.env.EVAKA_CUSTOMER_SAML_ISSUER ??
            ifNodeEnv(['local', 'test'], 'evaka-customer')
        ),
        publicCert: required(
          process.env.EVAKA_CUSTOMER_SAML_PUBLIC_CERT ??
            ifNodeEnv(['local', 'test'], 'config/test-cert/keycloak-local.pem')
        ),
        privateCert: required(
          process.env.EVAKA_CUSTOMER_SAML_PRIVATE_CERT ??
            ifNodeEnv(['local', 'test'], 'config/test-cert/saml-private.pem')
        ),
        validateInResponseTo: true
      }
    : undefined

const titaniaUsername = process.env.EVAKA_TITANIA_USERNAME
export const titaniaConfig = titaniaUsername
  ? {
      username: titaniaUsername,
      password: required(process.env.EVAKA_TITANIA_PASSWORD)
    }
  : undefined
