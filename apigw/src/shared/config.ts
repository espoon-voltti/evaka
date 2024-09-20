// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { ValidateInResponseTo } from '@node-saml/node-saml'
import { RedisClientOptions } from 'redis'

import { TrustedCertificates } from './certificates.js'

type EnvVariables = typeof envVariables
const envVariables = {
  // ----- Miscellaneous configuration -----
  /**
   * Environment name.
   *
   * This gets passed to log events in the `env` field, and to DataDog config `env` field.
   */
  VOLTTI_ENV: unset<string>(),
  /**
   * The port the HTTP server listens on
   */
  HTTP_PORT: 3000,
  /**
   * If true, adds a proxy route to the evaka-service dev API.
   *
   * **Do not enable in production**
   */
  ENABLE_DEV_API: false,

  // ----- Session configuration -----
  /**
   * @deprecated use CITIZEN_COOKIE_SECRET and EMPLOYEE_COOKIE_SECRET instead
   */
  COOKIE_SECRET: unset<string>(),
  /**
   * Default timeout (in minutes) for idle sessions.
   *
   * This timeout can be overridden for specific session types by more specific environment variables.
   */
  SESSION_TIMEOUT_MINUTES: 32,
  /**
   * If true, all cookies are set with the `Secure` flag.
   *
   * **Do not disable in production**
   */
  USE_SECURE_COOKIES: true,
  /**
   * Secret used for signing citizen session cookies.
   *
   * Recommended to be a string of at least 32 random characters.
   */
  CITIZEN_COOKIE_SECRET: unset<string>(),
  /**
   * Timeout (in minutes) for idle citizen sessions.
   *
   * If set, overrides the value of SESSION_TIMEOUT_MINUTES for these sessions
   */
  CITIZEN_SESSION_TIMEOUT_MINUTES: unset<number>(),
  /**
   * Secret used for signing employee / employee mobile session cookies.
   *
   * Recommended to be a string of at least 32 random characters.
   */
  EMPLOYEE_COOKIE_SECRET: unset<string>(),
  /**
   * Timeout (in minutes) for idle employee / employee mobile sessions.
   *
   * If set, overrides the value of SESSION_TIMEOUT_MINUTES for these sessions
   */
  EMPLOYEE_SESSION_TIMEOUT_MINUTES: unset<number>(),
  /**
   * Timeout (in seconds) for employee mobile pin login sessions.
   */
  PIN_SESSION_TIMEOUT_SECONDS: 10 * 60,

  // ----- Redis configuration -----
  /**
   * Redis server hostname
   */
  REDIS_HOST: unset<string>(),
  /**
   * Redis server port
   */
  REDIS_PORT: unset<number>(),
  /**
   * Redis password
   */
  REDIS_PASSWORD: unset<string>(),
  /**
   * If true, connections to Redis are made without using TLS encryption
   */
  REDIS_DISABLE_SECURITY: false,
  /**
   * Redis server TLS hostname.
   *
   * Required if REDIS_DISABLE_SECURITY is false.
   */
  REDIS_TLS_SERVER_NAME: unset<string>(),

  // ----- Logging / debugging -----
  /**
   * Value for the `appBuild` field in log events.
   *
   * Normally set to a fixed value during Docker container build.
   */
  APP_BUILD: 'UNDEFINED',
  /**
   * Value for the `appCommit` field in log events.
   *
   * Normally set to a fixed value during Docker container build.
   */
  APP_COMMIT: 'UNDEFINED',
  /**
   * Value for the `hostIp` field in log events.
   *
   * Normally set by Docker container entrypoint script at container startup.
   */
  HOST_IP: 'UNDEFINED',
  /**
   * If true, includes the error message in all error responses.
   *
   * **Do not enable in production**
   */
  INCLUDE_ALL_ERROR_MESSAGES: false,
  /**
   * If true, outputs pretty-printed simple logs instead of JSON.
   */
  PRETTY_LOGS: false,

  // ----- DataDog tracing/profiling support -----
  /**
   * If true, enables DataDog tracing.
   */
  DD_TRACE_ENABLED: unset<boolean>(),
  /**
   * If true, enables DataDog profiling.
   */
  DD_PROFILING_ENABLED: unset<boolean>(),
  /**
   * Hostname for the DataDog agent
   */
  DD_TRACE_AGENT_HOSTNAME: 'localhost',
  /**
   * Port for the DataDog agent
   */
  DD_TRACE_AGENT_PORT: 8126,

  // ----- JWT token configuration -----
  /**
   * Path to a private key PEM file for JWT signing
   */
  JWT_PRIVATE_KEY: unset<string>(),
  /**
   * Value for the `keyid` field in generated JWT tokens
   */
  JWT_KID: 'evaka-api-gw',
  /**
   * If true, automatically refreshes JWT tokens when near expiration.
   *
   * **Do not disable in production**
   */
  JWT_REFRESH_ENABLED: true,

  // ----- eVaka frontend/backend URLs -----
  /**
   * Base URL for eVaka frontend (e.g. https://evaka.example.com)
   */
  EVAKA_BASE_URL: unset<string>(),
  /**
   * Base URL for evaka-service backend (e.g. http://evaka-service.example.local)
   */
  EVAKA_SERVICE_URL: unset<string>(),

  // ----- Active Directory SAML authentication -----
  /**
   * @deprecated Use `AD_MOCK` instead
   */
  DEV_LOGIN: unset<boolean>(),
  /**
   * If true, uses a mock authentication provider for AD, meant for local development only.
   *
   * **Do not enable in production**
   */
  AD_MOCK: false,
  /**
   * Hard-coded prefix for external IDs that originate from AD SAML authentication.
   *
   * When an AD user authenticates, a full external ID that looks like "prefix:id" is sent to evaka-service.
   */
  AD_SAML_EXTERNAL_ID_PREFIX: 'espoo-ad',
  /**
   * SAML authentication response attribute containing a persistent identifier for AD users.
   *
   * This attribute must be present in the SAML response or authentication will fail. When an AD user authenticates,
   * a full external ID that looks like "prefix:id" is sent to evaka-service.
   */
  AD_USER_ID_KEY:
    'http://schemas.microsoft.com/identity/claims/objectidentifier',
  /**
   * If true, assumes SAML responses from AD are encrypted using our SP public certificate.
   *
   * This depends on how AD is configured and may not be necessary.
   */
  AD_DECRYPT_ASSERTIONS: false,
  /**
   * URN for the NameIDFormat used in AD SAML requests
   */
  AD_NAME_ID_FORMAT: 'urn:oasis:names:tc:SAML:2.0:nameid-format:transient',
  /**
   * SAML service provider callback URL for AD authentication.
   *
   * This is the URL for our SAML authentication response endpoint
   * (e.g. https://evaka.example.com/api/internal/auth/saml/login/callback)
   */
  AD_SAML_CALLBACK_URL: unset<string>(),
  /**
   * SAML identity provider entrypoint URL for AD authentication.
   *
   * This is the URL for the AD-side SAML authentication request endpoint.
   * (e.g. https://login.microsoftonline.com/tenant-id/saml2)
   */
  AD_SAML_ENTRYPOINT_URL: unset<string>(),
  /**
   * SAML identity provider logout URL for AD authentication.
   *
   * This is the URL for the AD-side SAML logout request endpoint.
   * (e.g. https://login.microsoftonline.com/tenant-id/saml2)
   */
  AD_SAML_LOGOUT_URL: unset<string>(),
  /**
   * SAML issuer for AD authentication.
   *
   * This value should match the entityID specified in the SAML metadata sent to AD.
   */
  AD_SAML_ISSUER: unset<string>(),
  /**
   * Comma-separated paths to SAML IDP public certificates in PEM format.
   *
   * If multiple certificates are provided, SAML messages signed by any of them are accepted.
   */
  AD_SAML_PUBLIC_CERT: unset<string[]>(),
  /**
   * Path to SAML SP private certificate in PEM format
   */
  AD_SAML_PRIVATE_CERT: unset<string>(),

  // ----- Suomi.fi SAML authentication -----
  /**
   * @deprecated use SFI_MODE=mock instead
   */
  SFI_MOCK: unset<boolean>(),
  /**
   * Chooses the SFI authentication mode and environment.
   *
   * prod = use SAML and Suomi.fi production environment
   * test = use SAML and Suomi.fi test environment
   * mock = use a mock implementation intended for local development only
   *
   * **Do not use "mock" in production**
   */
  SFI_MODE: unset<'test' | 'prod' | 'mock'>(),
  /**
   * SAML service provider callback URL for SFI authentication.
   *
   * This is the URL for our SAML authentication response endpoint
   * (e.g. https://evaka.example.com/api/application/auth/saml/login/callback)
   */
  SFI_SAML_CALLBACK_URL: unset<string>(),
  /**
   * SAML identity provider entrypoint URL for SFI authentication.
   *
   * If SFI_MODE is set, this value is automatically set and is not required.
   * This is the URL for the SFI-side SAML authentication request endpoint.
   * (e.g. https://tunnistautuminen.suomi.fi/idp/profile/SAML2/Redirect/SSO)
   */
  SFI_SAML_ENTRYPOINT: unset<string>(),
  /**
   * SAML identity provider entrypoint URL for SFI authentication.
   *
   * If SFI_MODE is set, this value is automatically set and is not required.
   * This is the URL for the SFI-side SAML logout request endpoint.
   * (e.g. https://tunnistautuminen.suomi.fi/idp/profile/SAML2/Redirect/SLO)
   */
  SFI_SAML_LOGOUT_URL: unset<string>(),
  /**
   * SAML issuer for SFI authentication.
   *
   * This value should match the entityID specified in the SAML metadata sent to SFI.
   */
  SFI_SAML_ISSUER: unset<string>(),
  /**
   * Comma-separated paths to SAML IDP public certificates in PEM format or known builtin certificate names.
   *
   * If SFI_MODE is set, the currently valid builtin certificates are automatically used and setting this variable is
   * not required. If multiple certificates are provided, SAML messages signed by any of them are accepted.
   */
  SFI_SAML_PUBLIC_CERT: unset<string[]>(),
  /**
   * Path to SAML SP private certificate in PEM format
   */
  SFI_SAML_PRIVATE_CERT: unset<string>(),

  // ----- eVaka employee Keycloak SAML authentication -----
  /**
   * SAML service provider callback URL for citizen Keycloak authentication.
   *
   * This is the URL for our SAML authentication response endpoint
   * (e.g. https://evaka.example.com/api/internal/auth/evaka/login/callback)
   */
  EVAKA_SAML_CALLBACK_URL: unset<string>(),
  /**
   * SAML identity provider entrypoint URL for citizen Keycloak authentication.
   *
   * This is the URL for the Keycloak-side SAML authentication request endpoint.
   * (e.g. https://evaka.example.com/auth/realms/evaka/protocol/saml)
   */
  EVAKA_SAML_ENTRYPOINT: unset<string>(),
  /**
   * SAML issuer for employee Keycloak authentication.
   *
   * This value should match the "Service provider entity ID" field in Keycloak configuration.
   */
  EVAKA_SAML_ISSUER: unset<string>(),
  /**
   * Comma-separated paths to SAML IDP public certificates in PEM format.
   *
   * If multiple certificates are provided, SAML messages signed by any of them are accepted.
   */
  EVAKA_SAML_PUBLIC_CERT: unset<string[]>(),
  /**
   * Path to SAML SP private certificate in PEM format
   */
  EVAKA_SAML_PRIVATE_CERT: unset<string>(),

  // ----- eVaka citizen Keycloak SAML authentication -----
  /**
   * SAML service provider callback URL for citizen Keycloak authentication.
   *
   * This is the URL for our SAML authentication response endpoint
   * (e.g. https://evaka.example.com/api/application/auth/evaka-customer/login/callback)
   */
  EVAKA_CUSTOMER_SAML_CALLBACK_URL: unset<string>(),
  /**
   * SAML identity provider entrypoint URL for citizen Keycloak authentication.
   *
   * This is the URL for the Keycloak-side SAML authentication request endpoint.
   * (e.g. https://evaka.example.com/auth/realms/evaka-customer/protocol/saml)
   */
  EVAKA_CUSTOMER_SAML_ENTRYPOINT: unset<string>(),
  /**
   * SAML issuer for citizen Keycloak authentication.
   *
   * This value should match the "Service provider entity ID" field in Keycloak configuration.
   */
  EVAKA_CUSTOMER_SAML_ISSUER: unset<string>(),
  /**
   * Comma-separated paths to SAML IDP public certificates in PEM format.
   *
   * If multiple certificates are provided, SAML messages signed by any of them are accepted.
   */
  EVAKA_CUSTOMER_SAML_PUBLIC_CERT: unset<string[]>(),
  /**
   * Path to SAML SP private certificate in PEM format
   */
  EVAKA_CUSTOMER_SAML_PRIVATE_CERT: unset<string>(),

  // ----- Titania integration -----
  /**
   * The username Titania uses to authenticate with eVaka using Basic Auth
   */
  EVAKA_TITANIA_USERNAME: unset<string>(),
  /**
   * The password Titania uses to authenticate with eVaka using Basic Auth
   */
  EVAKA_TITANIA_PASSWORD: unset<string>(),

  // ----- Digitransit API integration -----
  /**
   * If true, enables proxying to Digitransit API
   */
  DIGITRANSIT_API_ENABLED: true,
  /**
   * Base URL for the Digitransit API
   */
  DIGITRANSIT_API_URL: 'https://api.digitransit.fi',
  /**
   * API key for the Digitransit API
   */
  DIGITRANSIT_API_KEY: unset<string>()
}

// helper function to specify the type of undefined without casting
function unset<T>(): T | undefined {
  return undefined
}

/**
 * Returns environment variable overrides that are used only in local development
 */
function createLocalDevelopmentOverrides(): Partial<EnvVariables> {
  /**
   * True if we are in a local development environment
   */
  const isLocal = process.env.NODE_ENV === 'local'
  /**
   * True if we are in an automated test
   */
  const isTest = process.env.NODE_ENV === 'test'

  return isLocal || isTest
    ? {
        VOLTTI_ENV: 'local',
        ENABLE_DEV_API: true,

        CITIZEN_COOKIE_SECRET: 'A very hush hush cookie secret.',
        EMPLOYEE_COOKIE_SECRET: 'A very hush hush cookie secret.',
        USE_SECURE_COOKIES: false,

        REDIS_HOST: '127.0.0.1',
        REDIS_PORT: 6379,
        REDIS_DISABLE_SECURITY: true,

        INCLUDE_ALL_ERROR_MESSAGES: true,
        PRETTY_LOGS: isLocal,

        JWT_PRIVATE_KEY: 'config/test-cert/jwt_private_key.pem',
        JWT_REFRESH_ENABLED: !isTest,

        EVAKA_BASE_URL: 'local',
        EVAKA_SERVICE_URL: 'http://localhost:8888',

        AD_MOCK: true,
        SFI_MODE: 'mock',

        EVAKA_SAML_CALLBACK_URL:
          'http://localhost:9099/api/internal/auth/evaka/login/callback',
        EVAKA_SAML_ENTRYPOINT:
          'http://localhost:8080/auth/realms/evaka/protocol/saml',
        EVAKA_SAML_ISSUER: 'evaka',
        EVAKA_SAML_PUBLIC_CERT: ['config/test-cert/keycloak-local.pem'],
        EVAKA_SAML_PRIVATE_CERT: 'config/test-cert/saml-private.pem',

        EVAKA_CUSTOMER_SAML_CALLBACK_URL:
          'http://localhost:9099/api/application/auth/evaka-customer/login/callback',
        EVAKA_CUSTOMER_SAML_ENTRYPOINT:
          'http://localhost:8080/auth/realms/evaka-customer/protocol/saml',
        EVAKA_CUSTOMER_SAML_ISSUER: 'evaka-customer',
        EVAKA_CUSTOMER_SAML_PUBLIC_CERT: [
          'config/test-cert/keycloak-local.pem'
        ],
        EVAKA_CUSTOMER_SAML_PRIVATE_CERT: 'config/test-cert/saml-private.pem',

        DIGITRANSIT_API_ENABLED: false
      }
    : {}
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

const defaultEnvVariables: EnvVariables = {
  ...envVariables,
  ...createLocalDevelopmentOverrides()
}

const optional = <K extends keyof EnvVariables>(
  key: K,
  parser: Parser<EnvVariables[K]>
): EnvVariables[K] | undefined =>
  parseEnv(key, (value) => (value ? parser(value) : defaultEnvVariables[key]))

const required = <K extends keyof EnvVariables>(
  key: K,
  parser: Parser<NonNullable<EnvVariables[K]>>
): NonNullable<EnvVariables[K]> =>
  parseEnv(key, (value) =>
    value
      ? parser(value)
      : nonNullable(defaultEnvVariables[key], `${key} must be set`)
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
    optional('DEV_LOGIN', parseBoolean) ?? required('AD_MOCK', parseBoolean)
  const adType = adMock ? 'mock' : 'saml'
  const ad: Config['ad'] = {
    externalIdPrefix: required('AD_SAML_EXTERNAL_ID_PREFIX', unchanged),
    userIdKey: required('AD_USER_ID_KEY', unchanged),
    ...(adType !== 'saml'
      ? { type: adType }
      : {
          type: adType,
          saml: {
            callbackUrl: required('AD_SAML_CALLBACK_URL', unchanged),
            entryPoint: required('AD_SAML_ENTRYPOINT_URL', unchanged),
            logoutUrl: required('AD_SAML_LOGOUT_URL', unchanged),
            issuer: required('AD_SAML_ISSUER', unchanged),
            publicCert: required('AD_SAML_PUBLIC_CERT', parseArray(unchanged)),
            privateCert: required('AD_SAML_PRIVATE_CERT', unchanged),
            validateInResponseTo: ValidateInResponseTo.always,
            decryptAssertions: required('AD_DECRYPT_ASSERTIONS', parseBoolean),
            nameIdFormat: required('AD_NAME_ID_FORMAT', unchanged)
          }
        })
  }

  const sfiMock = optional('SFI_MOCK', parseBoolean)
  const sfiMode = optional(
    'SFI_MODE',
    parseEnum(['test', 'prod', 'mock'] as const)
  )
  const sfiType = sfiMode === 'mock' || sfiMock ? 'mock' : 'saml'
  const sfiDefaults = sfiMode ? sfiDefaultsForMode(sfiMode) : undefined

  const sfi: Config['sfi'] =
    sfiType !== 'saml'
      ? { type: sfiType }
      : {
          type: sfiType,
          saml: {
            callbackUrl: required('SFI_SAML_CALLBACK_URL', unchanged),
            entryPoint: nonNullable(
              optional('SFI_SAML_ENTRYPOINT', unchanged) ??
                sfiDefaults?.entryPoint,
              'Either SFI_ENV or SFI_SAML_ENTRYPOINT must be set'
            ),
            logoutUrl: nonNullable(
              optional('SFI_SAML_LOGOUT_URL', unchanged) ??
                sfiDefaults?.logoutUrl,
              'Either SFI_ENV or SFI_SAML_LOGOUT_URL must be set'
            ),
            issuer: required('SFI_SAML_ISSUER', unchanged),
            publicCert: nonNullable(
              optional('SFI_SAML_PUBLIC_CERT', parseArray(unchanged)) ??
                sfiDefaults?.publicCert,
              'Either SFI_ENV or SFI_SAML_PUBLIC_CERT must be set'
            ),
            privateCert: required('SFI_SAML_PRIVATE_CERT', unchanged),
            validateInResponseTo: ValidateInResponseTo.always,
            decryptAssertions: true
          }
        }

  const keycloakEmployeeCallbackUrl = optional(
    'EVAKA_SAML_CALLBACK_URL',
    unchanged
  )

  const keycloakEmployee: EvakaSamlConfig | undefined =
    keycloakEmployeeCallbackUrl
      ? {
          callbackUrl: keycloakEmployeeCallbackUrl,
          entryPoint: required('EVAKA_SAML_ENTRYPOINT', unchanged),
          // NOTE: Same as entrypoint, on purpose
          logoutUrl: required('EVAKA_SAML_ENTRYPOINT', unchanged),
          issuer: required('EVAKA_SAML_ISSUER', unchanged),
          publicCert: required('EVAKA_SAML_PUBLIC_CERT', parseArray(unchanged)),
          privateCert: required('EVAKA_SAML_PRIVATE_CERT', unchanged),
          validateInResponseTo: ValidateInResponseTo.always,
          decryptAssertions: true
        }
      : undefined

  const keycloakCitizenCallbackUrl = optional(
    'EVAKA_CUSTOMER_SAML_CALLBACK_URL',
    unchanged
  )

  const keycloakCitizen: EvakaSamlConfig | undefined =
    keycloakCitizenCallbackUrl
      ? {
          callbackUrl: keycloakCitizenCallbackUrl,
          entryPoint: required('EVAKA_CUSTOMER_SAML_ENTRYPOINT', unchanged),
          logoutUrl: required('EVAKA_CUSTOMER_SAML_ENTRYPOINT', unchanged),
          issuer: required('EVAKA_CUSTOMER_SAML_ISSUER', unchanged),
          publicCert: required(
            'EVAKA_CUSTOMER_SAML_PUBLIC_CERT',
            parseArray(unchanged)
          ),
          privateCert: required('EVAKA_CUSTOMER_SAML_PRIVATE_CERT', unchanged),
          validateInResponseTo: ValidateInResponseTo.always,
          decryptAssertions: true
        }
      : undefined

  const legacyCookieSecret = optional('COOKIE_SECRET', unchanged)
  const useSecureCookies = required('USE_SECURE_COOKIES', parseBoolean)
  const defaultSessionTimeoutMinutes = required(
    'SESSION_TIMEOUT_MINUTES',
    parseInteger
  )

  return {
    citizen: {
      useSecureCookies,
      cookieSecret: nonNullable(
        optional('CITIZEN_COOKIE_SECRET', unchanged) ?? legacyCookieSecret,
        'Either COOKIE_SECRET or CITIZEN_COOKIE_SECRET must be set'
      ),
      sessionTimeoutMinutes:
        optional('CITIZEN_SESSION_TIMEOUT_MINUTES', parseInteger) ??
        defaultSessionTimeoutMinutes
    },
    employee: {
      useSecureCookies,
      cookieSecret: nonNullable(
        optional('EMPLOYEE_COOKIE_SECRET', unchanged) ?? legacyCookieSecret,
        'Either COOKIE_SECRET or EMPLOYEE_COOKIE_SECRET must be set'
      ),
      sessionTimeoutMinutes:
        optional('EMPLOYEE_SESSION_TIMEOUT_MINUTES', parseInteger) ??
        defaultSessionTimeoutMinutes
    },
    ad,
    sfi,
    redis: {
      host: optional('REDIS_HOST', unchanged),
      port: optional('REDIS_PORT', parseInteger),
      password: optional('REDIS_PASSWORD', unchanged),
      disableSecurity: required('REDIS_DISABLE_SECURITY', parseBoolean),
      tlsServerName: optional('REDIS_TLS_SERVER_NAME', unchanged)
    },
    keycloakEmployee,
    keycloakCitizen
  }
}

export const appBuild = required('APP_BUILD', unchanged)
export const appCommit = required('APP_COMMIT', unchanged)
export const hostIp = required('HOST_IP', unchanged)
export const includeAllErrorMessages = required(
  'INCLUDE_ALL_ERROR_MESSAGES',
  parseBoolean
)

export const tracingEnabled = optional('DD_TRACE_ENABLED', parseBoolean)
export const profilingEnabled = optional('DD_PROFILING_ENABLED', parseBoolean)
export const traceAgentHostname = optional('DD_TRACE_AGENT_HOSTNAME', unchanged)
export const traceAgentPort = optional('DD_TRACE_AGENT_PORT', parseInteger)

export const jwtPrivateKey = required('JWT_PRIVATE_KEY', unchanged)
export const jwtRefreshEnabled = required('JWT_REFRESH_ENABLED', parseBoolean)

export const serviceName = 'evaka-api-gw'
export const jwtKid = required('JWT_KID', unchanged)

export const evakaBaseUrl = required('EVAKA_BASE_URL', unchanged)
export const evakaServiceUrl = required('EVAKA_SERVICE_URL', unchanged)
export const useSecureCookies = required('USE_SECURE_COOKIES', parseBoolean)

export const prettyLogs = required('PRETTY_LOGS', parseBoolean)

export const volttiEnv = optional('VOLTTI_ENV', unchanged)

export const httpPort = required('HTTP_PORT', parseInteger)

export const pinSessionTimeoutSeconds = required(
  'PIN_SESSION_TIMEOUT_SECONDS',
  parseInteger
)

export const enableDevApi = required('ENABLE_DEV_API', parseBoolean)

const titaniaUsername = optional('EVAKA_TITANIA_USERNAME', unchanged)
export const titaniaConfig = titaniaUsername
  ? {
      username: titaniaUsername,
      password: required('EVAKA_TITANIA_PASSWORD', unchanged)
    }
  : undefined

export const digitransitApiEnabled = required(
  'DIGITRANSIT_API_ENABLED',
  parseBoolean
)
export const digitransitApiUrl = required('DIGITRANSIT_API_URL', unchanged)
export const digitransitApiKey = optional('DIGITRANSIT_API_KEY', unchanged)
