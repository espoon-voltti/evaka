// Original source: https://github.com/DefinitelyTyped/DefinitelyTyped/tree/60c738767233468875d08171818ac48f322dbadb/types/passport-saml
// Original license:
// SPDX-FileCopyrightText: 2012-2019 Chris Barth <https://github.com/cjbarth>, Damian Assennato <https://github.com/dassennato>, Karol Samborski <https://github.com/ksamborski>, Jose Colella <https://github.com/josecolella>
//
// SPDX-License-Identifier: MIT
//
// Modifications:
// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

declare module 'passport-saml' {
  import type passport from 'passport'
  import type express from 'express'

  export interface CacheItem {
    createdAt: number
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    value: any
  }

  export interface CacheProvider {
    save(
      key: string | null,
      // eslint-disable-next-line @typescript-eslint/no-explicit-any
      value: any,
      callback: (err: Error | null, cacheItem: CacheItem | null) => void | null
    ): void
    get(
      key: string,
      // eslint-disable-next-line @typescript-eslint/no-explicit-any
      callback: (err: Error | null, value: any | null) => void | null
    ): void
    remove(
      key: string,
      callback: (err: Error | null, key: string | null) => void | null
    ): void
  }

  export type VerifiedCallback = (
    err: Error | null,
    user?: object,
    info?: object
  ) => void

  export type VerifyWithRequest = (
    req: express.Request,
    profile: Profile,
    done: VerifiedCallback
  ) => void

  export type VerifyWithoutRequest = (
    profile: Profile,
    done: VerifiedCallback
  ) => void

  export class Strategy extends passport.Strategy {
    constructor(
      config: SamlConfig,
      verify: VerifyWithRequest | VerifyWithoutRequest
    )
    authenticate(
      req: express.Request,
      options: AuthenticateOptions | AuthorizeOptions
    ): void
    logout(
      req: express.Request,
      callback: (err: Error | null, url?: string) => void
    ): void
    generateServiceProviderMetadata(
      decryptionCert: string | null,
      signingCert?: string | null
    ): string
  }

  export type CertCallback = (
    callback: (err: Error | null, cert?: string | string[]) => void
  ) => void

  export interface SamlConfig {
    // Core
    callbackUrl?: string
    path?: string
    protocol?: string
    host?: string
    entryPoint?: string
    issuer?: string
    privateCert?: string
    cert?: string | string[] | CertCallback
    decryptionPvk?: string
    signatureAlgorithm?: 'sha1' | 'sha256' | 'sha512'

    // Additional SAML behaviors
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    additionalParams?: any
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    additionalAuthorizeParams?: any
    identifierFormat?: string
    acceptedClockSkewMs?: number
    attributeConsumingServiceIndex?: string
    disableRequestedAuthnContext?: boolean
    authnContext?: string
    forceAuthn?: boolean
    skipRequestCompression?: boolean
    authnRequestBinding?: string
    RACComparison?: 'exact' | 'minimum' | 'maximum' | 'better'
    providerName?: string
    passive?: boolean
    idpIssuer?: string
    audience?: string

    // InResponseTo Validation
    validateInResponseTo?: boolean
    requestIdExpirationPeriodMs?: number
    cacheProvider?: CacheProvider

    // Passport
    name?: string
    passReqToCallback?: boolean

    // Logout
    logoutUrl?: string
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    additionalLogoutParams?: any
    logoutCallbackUrl?: string
  }

  export interface AuthenticateOptions extends passport.AuthenticateOptions {
    additionalParams?: object
  }

  export interface AuthorizeOptions extends AuthenticateOptions {
    samlFallback?: string
  }

  export type Profile = {
    issuer?: string
    sessionIndex?: string
    nameID?: string
    nameIDFormat?: string
    nameQualifier?: string
    spNameQualifier?: string
    ID?: string
    mail?: string // InCommon Attribute urn:oid:0.9.2342.19200300.100.1.3
    email?: string // `mail` if not present in the assertion
    getAssertionXml(): string // get the raw assertion XML
    getAssertion(): object // get the assertion XML parsed as a JavaScript object
    getSamlResponseXml(): string // get the raw SAML response XML
  } & {
    [attributeName: string]: unknown // arbitrary `AttributeValue`s
  }

  namespace MultiSamlStrategy {
    type SamlOptionsCallback = (
      err: Error | null,
      samlOptions?: SamlConfig
    ) => void

    interface MultiSamlConfig extends SamlConfig {
      getSamlOptions(req: express.Request, callback: SamlOptionsCallback): void
    }
  }

  export class MultiSamlStrategy extends Strategy {
    constructor(
      config: MultiSamlStrategy.MultiSamlConfig,
      verify: VerifyWithRequest | VerifyWithoutRequest
    )
    generateServiceProviderMetadata(
      decryptionCert: string | null,
      signingCert?: string | null
    ): never
    generateServiceProviderMetadata(
      req: express.Request,
      decryptionCert: string | null,
      signingCert: string | null,
      callback: (err: Error | null, metadata?: string) => void
    ): string
  }
}
