// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { ValidateInResponseTo } from '@node-saml/node-saml'
import { RedisClientOptions } from 'redis'

import { TrustedCertificates } from './certificates.js'

export interface EnvVariables {
  VOLTTI_ENV?: string
  HTTP_PORT?: number
  ENABLE_DEV_API?: boolean

  COOKIE_SECRET?: string
  USE_SECURE_COOKIES?: boolean
  SESSION_TIMEOUT_MINUTES?: number
  CITIZEN_COOKIE_SECRET?: string
  CITIZEN_SESSION_TIMEOUT_MINUTES?: number
  EMPLOYEE_COOKIE_SECRET?: string
  EMPLOYEE_SESSION_TIMEOUT_MINUTES?: number
  PIN_SESSION_TIMEOUT_SECONDS?: number

  REDIS_HOST?: string
  REDIS_PORT?: number
  REDIS_PASSWORD?: string
  REDIS_TLS_SERVER_NAME?: string
  REDIS_DISABLE_SECURITY?: boolean

  APP_BUILD?: string
  APP_COMMIT?: string
  HOST_IP?: string
  INCLUDE_ALL_ERROR_MESSAGES?: boolean
  PRETTY_LOGS?: boolean

  DD_TRACE_ENABLED?: boolean
  DD_PROFILING_ENABLED?: boolean
  DD_TRACE_AGENT_HOSTNAME?: string
  DD_TRACE_AGENT_PORT?: number

  JWT_PRIVATE_KEY?: string
  JWT_KID?: string
  JWT_REFRESH_ENABLED?: boolean

  EVAKA_BASE_URL?: string
  EVAKA_SERVICE_URL?: string

  DEV_LOGIN?: boolean
  AD_MOCK?: boolean
  AD_SAML_EXTERNAL_ID_PREFIX?: string
  AD_USER_ID_KEY?: string
  AD_SAML_CALLBACK_URL?: string
  AD_SAML_ENTRYPOINT_URL?: string
  AD_SAML_LOGOUT_URL?: string
  AD_SAML_ISSUER?: string
  AD_SAML_PUBLIC_CERT?: string[]
  AD_SAML_PRIVATE_CERT?: string
  AD_DECRYPT_ASSERTIONS?: boolean
  AD_NAME_ID_FORMAT?: string

  SFI_MOCK?: boolean
  SFI_MODE?: 'test' | 'prod' | 'mock'
  SFI_SAML_CALLBACK_URL?: string
  SFI_SAML_ENTRYPOINT?: string
  SFI_SAML_LOGOUT_URL?: string
  SFI_SAML_ISSUER?: string
  SFI_SAML_PUBLIC_CERT?: string[]
  SFI_SAML_PRIVATE_CERT?: string

  EVAKA_SAML_CALLBACK_URL?: string
  EVAKA_SAML_ENTRYPOINT?: string
  EVAKA_SAML_ISSUER?: string
  EVAKA_SAML_PUBLIC_CERT?: string[]
  EVAKA_SAML_PRIVATE_CERT?: string

  EVAKA_CUSTOMER_SAML_CALLBACK_URL?: string
  EVAKA_CUSTOMER_SAML_ENTRYPOINT?: string
  EVAKA_CUSTOMER_SAML_ISSUER?: string
  EVAKA_CUSTOMER_SAML_PUBLIC_CERT?: string[]
  EVAKA_CUSTOMER_SAML_PRIVATE_CERT?: string

  EVAKA_TITANIA_USERNAME?: string
  EVAKA_TITANIA_PASSWORD?: string

  DIGITRANSIT_API_ENABLED?: boolean
  DIGITRANSIT_API_URL?: string
  DIGITRANSIT_API_KEY?: string
}

export interface Config {
  citizen: SessionConfig
  employee: SessionConfig
  ad: {
    externalIdPrefix: string
    userIdKey: string
  } & (
    | { type: 'mock' | 'disabled' }
    | {
        type: 'saml'
        saml: EvakaSamlConfig
      }
  )
  sfi: { type: 'mock' | 'disabled' } | { type: 'saml'; saml: EvakaSamlConfig }
  keycloakEmployee: EvakaSamlConfig | undefined
  keycloakCitizen: EvakaSamlConfig | undefined
  redis: {
    host: string | undefined
    port: number | undefined
    password: string | undefined
    tlsServerName: string | undefined
    disableSecurity: boolean
  }
}

export interface SessionConfig {
  useSecureCookies: boolean
  cookieSecret: string
  sessionTimeoutMinutes: number
}

export const toRedisClientOpts = (config: Config): RedisClientOptions => ({
  socket: {
    host: config.redis.host,
    port: config.redis.port,
    ...(config.redis.disableSecurity
      ? undefined
      : { tls: true, servername: config.redis.tlsServerName })
  },
  ...(config.redis.disableSecurity
    ? undefined
    : { password: config.redis.password })
})

export interface EvakaSamlConfig {
  callbackUrl: string
  entryPoint: string
  logoutUrl: string
  issuer: string
  publicCert: string | string[]
  privateCert: string
  validateInResponseTo: ValidateInResponseTo
  decryptAssertions: boolean
  nameIdFormat?: string | undefined
}

/**
 * Creates default fallback values for environment variables.
 *
 * Some defaults depend on the value of the NODE_ENV environment variable
 */
function createDefaultEnvs(): EnvVariables {
  /**
   * True if we are in a local development environment
   */
  const isLocal = process.env.NODE_ENV === 'local'
  /**
   * True if we are in an automated test
   */
  const isTest = process.env.NODE_ENV === 'test'

  return {
    VOLTTI_ENV: isLocal ? 'local' : undefined,
    HTTP_PORT: 3000,
    ENABLE_DEV_API: isLocal || isTest,

    COOKIE_SECRET:
      isLocal || isTest ? 'A very hush hush cookie secret.' : undefined,
    USE_SECURE_COOKIES: !isLocal && !isTest,
    PIN_SESSION_TIMEOUT_SECONDS: 10 * 60,

    ...(isLocal && {
      REDIS_HOST: '127.0.0.1',
      REDIS_PORT: 6379,
      REDIS_DISABLE_SECURITY: true
    }),

    INCLUDE_ALL_ERROR_MESSAGES: isLocal || isTest,
    PRETTY_LOGS: isLocal,

    JWT_PRIVATE_KEY:
      isLocal || isTest ? 'config/test-cert/jwt_private_key.pem' : undefined,
    JWT_REFRESH_ENABLED: !isTest,

    EVAKA_BASE_URL: isLocal || isTest ? 'local' : undefined,
    EVAKA_SERVICE_URL: isLocal || isTest ? 'http://localhost:8888' : undefined,

    AD_MOCK: isLocal || isTest,
    AD_SAML_EXTERNAL_ID_PREFIX: 'espoo-ad',
    AD_USER_ID_KEY:
      'http://schemas.microsoft.com/identity/claims/objectidentifier',
    AD_DECRYPT_ASSERTIONS: false,

    SFI_MODE: isLocal || isTest ? 'mock' : undefined,

    ...((isLocal || isTest) && {
      EVAKA_SAML_CALLBACK_URL: `http://localhost:9099/api/internal/auth/evaka/login/callback`,
      EVAKA_SAML_ENTRYPOINT:
        'http://localhost:8080/auth/realms/evaka/protocol/saml',
      EVAKA_SAML_ISSUER: 'evaka',
      EVAKA_SAML_PUBLIC_CERT: ['config/test-cert/keycloak-local.pem'],
      EVAKA_SAML_PRIVATE_CERT: 'config/test-cert/saml-private.pem',

      EVAKA_CUSTOMER_SAML_CALLBACK_URL: `http://localhost:9099/api/application/auth/evaka-customer/login/callback`,
      EVAKA_CUSTOMER_SAML_ENTRYPOINT:
        'http://localhost:8080/auth/realms/evaka-customer/protocol/saml',
      EVAKA_CUSTOMER_SAML_ISSUER: 'evaka-customer',
      EVAKA_CUSTOMER_SAML_PUBLIC_CERT: ['config/test-cert/keycloak-local.pem'],
      EVAKA_CUSTOMER_SAML_PRIVATE_CERT: 'config/test-cert/saml-private.pem'
    }),

    DIGITRANSIT_API_ENABLED: !isLocal && !isTest
  }
}

const defaultEnvs = createDefaultEnvs()

function nonNullable<T>(
  value: T | undefined,
  errorMessage: string
): NonNullable<T> {
  if (value == null) {
    throw new Error(errorMessage)
  }
  return value
}

type Parser<T> = (value: string) => T

const unchanged: Parser<string> = (value) => value

const parseInteger: Parser<number> = (value) => {
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

const parseBoolean: Parser<boolean> = (value) => {
  if (value in booleans) return booleans[value]
  throw new Error('Invalid boolean')
}

function parseEnum<T extends string>(variants: readonly T[]): Parser<T> {
  return (value) => {
    for (const variant of variants) {
      if (value === variant) return variant
    }
    throw new Error(`Invalid enum (expected one of ${variants.toString()})`)
  }
}

function parseArray<T>(elementParser: Parser<T>, separator = ','): Parser<T[]> {
  return (value) => value.split(separator).map(elementParser)
}

function parseEnv<K extends keyof EnvVariables, T>(
  key: K,
  f: (value: string | undefined) => T
): T {
  const value = process.env[key]?.trim()
  try {
    return f(value == null || value === '' ? undefined : value)
  } catch (err) {
    const message = err instanceof Error ? err.message : String(err)
    throw new Error(`${message}: ${key}=${value}`)
  }
}

const optionalEnv = <K extends keyof EnvVariables>(
  key: K,
  parser: Parser<EnvVariables[K]>
): EnvVariables[K] | undefined =>
  parseEnv(key, (value) => (value ? parser(value) : defaultEnvs[key]))

const requiredEnv = <K extends keyof EnvVariables>(
  key: K,
  parser: Parser<NonNullable<EnvVariables[K]>>
): NonNullable<EnvVariables[K]> =>
  parseEnv(key, (value) =>
    value ? parser(value) : nonNullable(defaultEnvs[key], `${key} must be set`)
  )

export type SfiMode = NonNullable<EnvVariables['SFI_MODE']>

function sfiDefaultsForMode(mode: SfiMode):
  | {
      entryPoint: string
      logoutUrl: string
      publicCert: TrustedCertificates[]
    }
  | undefined {
  switch (mode) {
    case 'prod':
      return {
        entryPoint:
          'https://tunnistautuminen.suomi.fi/idp/profile/SAML2/Redirect/SSO',
        logoutUrl:
          'https://tunnistautuminen.suomi.fi/idp/profile/SAML2/Redirect/SLO',
        publicCert: [
          'saml-signing.idp.tunnistautuminen.suomi.fi.2024.pem',
          'saml-signing.idp.tunnistautuminen.suomi.fi.2022.pem'
        ]
      }
    case 'test':
      return {
        entryPoint:
          'https://testi.apro.tunnistus.fi/idp/profile/SAML2/Redirect/SSO',
        logoutUrl:
          'https://testi.apro.tunnistus.fi/idp/profile/SAML2/Redirect/SLO',
        publicCert: [
          'saml-signing-testi.apro.tunnistus.fi.2024.pem',
          'saml-signing-testi.apro.tunnistus.fi.2022.pem'
        ]
      }
    case 'mock':
      return undefined
  }
}

export function configFromEnv(): Config {
  const adMock =
    optionalEnv('AD_MOCK', parseBoolean) ??
    optionalEnv('DEV_LOGIN', parseBoolean) ??
    false
  const adType = adMock ? 'mock' : 'saml'
  const ad: Config['ad'] = {
    externalIdPrefix: requiredEnv('AD_SAML_EXTERNAL_ID_PREFIX', unchanged),
    userIdKey: requiredEnv('AD_USER_ID_KEY', unchanged),
    ...(adType !== 'saml'
      ? { type: adType }
      : {
          type: adType,
          saml: {
            callbackUrl: requiredEnv('AD_SAML_CALLBACK_URL', unchanged),
            entryPoint: requiredEnv('AD_SAML_ENTRYPOINT_URL', unchanged),
            logoutUrl: requiredEnv('AD_SAML_LOGOUT_URL', unchanged),
            issuer: requiredEnv('AD_SAML_ISSUER', unchanged),
            publicCert: requiredEnv(
              'AD_SAML_PUBLIC_CERT',
              parseArray(unchanged)
            ),
            privateCert: requiredEnv('AD_SAML_PRIVATE_CERT', unchanged),
            validateInResponseTo: ValidateInResponseTo.always,
            decryptAssertions: requiredEnv(
              'AD_DECRYPT_ASSERTIONS',
              parseBoolean
            ),
            nameIdFormat: requiredEnv('AD_NAME_ID_FORMAT', unchanged)
          }
        })
  }

  const sfiMock = requiredEnv('SFI_MOCK', parseBoolean)
  const sfiMode = optionalEnv(
    'SFI_MODE',
    parseEnum(['test', 'prod', 'mock'] as const)
  )
  const sfiType = sfiMock || sfiMode === 'mock' ? 'mock' : 'saml'
  const sfiDefaults = sfiMode && sfiDefaultsForMode(sfiMode)

  const sfi: Config['sfi'] =
    sfiType !== 'saml'
      ? { type: sfiType }
      : {
          type: sfiType,
          saml: {
            callbackUrl: requiredEnv('SFI_SAML_CALLBACK_URL', unchanged),
            entryPoint: nonNullable(
              optionalEnv('SFI_SAML_ENTRYPOINT', unchanged) ??
                sfiDefaults?.entryPoint,
              'Either SFI_ENV or SFI_SAML_ENTRYPOINT must be set'
            ),
            logoutUrl: nonNullable(
              optionalEnv('SFI_SAML_LOGOUT_URL', unchanged) ??
                sfiDefaults?.logoutUrl,
              'Either SFI_ENV or SFI_SAML_LOGOUT_URL must be set'
            ),
            issuer: requiredEnv('SFI_SAML_ISSUER', unchanged),
            publicCert: nonNullable(
              optionalEnv('SFI_SAML_PUBLIC_CERT', parseArray(unchanged)) ??
                sfiDefaults?.publicCert,
              'Either SFI_ENV or SFI_SAML_PUBLIC_CERT must be set'
            ),
            privateCert: requiredEnv('SFI_SAML_PRIVATE_CERT', unchanged),
            validateInResponseTo: ValidateInResponseTo.always,
            decryptAssertions: true
          }
        }

  const keycloakEmployeeCallbackUrl = optionalEnv(
    'EVAKA_SAML_CALLBACK_URL',
    unchanged
  )

  const keycloakEmployee: EvakaSamlConfig | undefined =
    keycloakEmployeeCallbackUrl
      ? {
          callbackUrl: keycloakEmployeeCallbackUrl,
          entryPoint: requiredEnv('EVAKA_SAML_ENTRYPOINT', unchanged),
          // NOTE: Same as entrypoint, on purpose
          logoutUrl: requiredEnv('EVAKA_SAML_ENTRYPOINT', unchanged),
          issuer: requiredEnv('EVAKA_SAML_ISSUER', unchanged),
          publicCert: requiredEnv(
            'EVAKA_SAML_PUBLIC_CERT',
            parseArray(unchanged)
          ),
          privateCert: requiredEnv('EVAKA_SAML_PRIVATE_CERT', unchanged),
          validateInResponseTo: ValidateInResponseTo.always,
          decryptAssertions: true
        }
      : undefined

  const keycloakCitizenCallbackUrl = optionalEnv(
    'EVAKA_CUSTOMER_SAML_CALLBACK_URL',
    unchanged
  )

  const keycloakCitizen: EvakaSamlConfig | undefined =
    keycloakCitizenCallbackUrl
      ? {
          callbackUrl: keycloakCitizenCallbackUrl,
          entryPoint: requiredEnv('EVAKA_CUSTOMER_SAML_ENTRYPOINT', unchanged),
          logoutUrl: requiredEnv('EVAKA_CUSTOMER_SAML_ENTRYPOINT', unchanged),
          issuer: requiredEnv('EVAKA_CUSTOMER_SAML_ISSUER', unchanged),
          publicCert: requiredEnv(
            'EVAKA_CUSTOMER_SAML_PUBLIC_CERT',
            parseArray(unchanged)
          ),
          privateCert: requiredEnv(
            'EVAKA_CUSTOMER_SAML_PRIVATE_CERT',
            unchanged
          ),
          validateInResponseTo: ValidateInResponseTo.always,
          decryptAssertions: true
        }
      : undefined

  const legacyCookieSecret = optionalEnv('COOKIE_SECRET', unchanged)
  const useSecureCookies = requiredEnv('USE_SECURE_COOKIES', parseBoolean)
  const defaultSessionTimeoutMinutes =
    optionalEnv('SESSION_TIMEOUT_MINUTES', parseInteger) ?? 32

  return {
    citizen: {
      useSecureCookies,
      cookieSecret: nonNullable(
        optionalEnv('CITIZEN_COOKIE_SECRET', unchanged) ?? legacyCookieSecret,
        'Either COOKIE_SECRET or CITIZEN_COOKIE_SECRET must be set'
      ),
      sessionTimeoutMinutes:
        optionalEnv('CITIZEN_SESSION_TIMEOUT_MINUTES', parseInteger) ??
        defaultSessionTimeoutMinutes
    },
    employee: {
      useSecureCookies,
      cookieSecret: nonNullable(
        optionalEnv('EMPLOYEE_COOKIE_SECRET', unchanged) ?? legacyCookieSecret,
        'Either COOKIE_SECRET or EMPLOYEE_COOKIE_SECRET must be set'
      ),
      sessionTimeoutMinutes:
        optionalEnv('EMPLOYEE_SESSION_TIMEOUT_MINUTES', parseInteger) ??
        defaultSessionTimeoutMinutes
    },
    ad,
    sfi,
    redis: {
      host: optionalEnv('REDIS_HOST', unchanged),
      port: optionalEnv('REDIS_PORT', parseInteger),
      password: optionalEnv('REDIS_PASSWORD', unchanged),
      tlsServerName: optionalEnv('REDIS_TLS_SERVER_NAME', unchanged),
      disableSecurity:
        optionalEnv('REDIS_DISABLE_SECURITY', parseBoolean) ?? false
    },
    keycloakEmployee,
    keycloakCitizen
  }
}

export const appBuild = optionalEnv('APP_BUILD', unchanged) ?? 'UNDEFINED'
export const appCommit = optionalEnv('APP_COMMIT', unchanged) ?? 'UNDEFINED'
export const hostIp = optionalEnv('HOST_IP', unchanged) ?? 'UNDEFINED'
export const includeAllErrorMessages = requiredEnv(
  'INCLUDE_ALL_ERROR_MESSAGES',
  parseBoolean
)

export const tracingEnabled = optionalEnv('DD_TRACE_ENABLED', parseBoolean)
export const profilingEnabled = optionalEnv(
  'DD_PROFILING_ENABLED',
  parseBoolean
)
export const traceAgentHostname =
  optionalEnv('DD_TRACE_AGENT_HOSTNAME', unchanged) ?? 'localhost'
export const traceAgentPort =
  optionalEnv('DD_TRACE_AGENT_PORT', parseInteger) ?? 8126

export const jwtPrivateKey = requiredEnv('JWT_PRIVATE_KEY', unchanged)
export const jwtRefreshEnabled = requiredEnv(
  'JWT_REFRESH_ENABLED',
  parseBoolean
)

export const serviceName = 'evaka-api-gw'
export const jwtKid = optionalEnv('JWT_KID', unchanged) ?? serviceName

export const evakaBaseUrl = requiredEnv('EVAKA_BASE_URL', unchanged)
export const evakaServiceUrl = requiredEnv('EVAKA_SERVICE_URL', unchanged)
export const useSecureCookies = requiredEnv('USE_SECURE_COOKIES', parseBoolean)

export const prettyLogs = requiredEnv('PRETTY_LOGS', parseBoolean)

export const volttiEnv = optionalEnv('VOLTTI_ENV', unchanged)

export const httpPort = requiredEnv('HTTP_PORT', parseInteger)

export const pinSessionTimeoutSeconds = requiredEnv(
  'PIN_SESSION_TIMEOUT_SECONDS',
  parseInteger
)

export const enableDevApi = requiredEnv('ENABLE_DEV_API', parseBoolean)

const titaniaUsername = optionalEnv('EVAKA_TITANIA_USERNAME', unchanged)
export const titaniaConfig = titaniaUsername
  ? {
      username: titaniaUsername,
      password: requiredEnv('EVAKA_TITANIA_PASSWORD', unchanged)
    }
  : undefined

export const digitransitApiEnabled = requiredEnv(
  'DIGITRANSIT_API_ENABLED',
  parseBoolean
)
export const digitransitApiUrl =
  optionalEnv('DIGITRANSIT_API_URL', unchanged) ?? 'https://api.digitransit.fi'
export const digitransitApiKey = optionalEnv('DIGITRANSIT_API_KEY', unchanged)
