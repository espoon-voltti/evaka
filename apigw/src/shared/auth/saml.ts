// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { z } from 'zod'
import {
  CacheProvider,
  Profile,
  SamlConfig,
  VerifiedCallback,
  VerifyWithoutRequest
} from 'passport-saml'
import { logWarn } from '../logging'
import { EvakaSessionUser } from './index'
import { EvakaSamlConfig } from '../config'
import { readFileSync } from 'fs'
import certificates, { TrustedCertificates } from '../certificates'

export function createSamlConfig(
  config: EvakaSamlConfig,
  cacheProvider?: CacheProvider
): SamlConfig {
  const privateCert = readFileSync(config.privateCert, {
    encoding: 'utf8'
  })
  const lookupPublicCert = (cert: string) =>
    cert in certificates
      ? certificates[cert as TrustedCertificates]
      : readFileSync(cert, {
          encoding: 'utf8'
        })
  const publicCert = Array.isArray(config.publicCert)
    ? config.publicCert.map(lookupPublicCert)
    : lookupPublicCert(config.publicCert)

  return {
    acceptedClockSkewMs: 0,
    audience: config.issuer,
    cacheProvider,
    callbackUrl: config.callbackUrl,
    cert: publicCert,
    disableRequestedAuthnContext: true,
    decryptionPvk: config.decryptAssertions ? privateCert : undefined,
    entryPoint: config.entryPoint,
    identifierFormat:
      config.nameIdFormat ??
      'urn:oasis:names:tc:SAML:2.0:nameid-format:transient',
    issuer: config.issuer,
    logoutUrl: config.logoutUrl,
    privateKey: privateCert,
    signatureAlgorithm: 'sha256',
    validateInResponseTo: config.validateInResponseTo
  }
}

export function toSamlVerifyFunction(
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  schema: z.ZodObject<any>,
  verify: (profile: Profile) => Promise<EvakaSessionUser>
): VerifyWithoutRequest {
  return (profile: Profile | null | undefined, done: VerifiedCallback) => {
    if (!profile) {
      done(new Error('No SAML profile'))
    } else {
      const parseResult = schema.safeParse(profile)
      if (!parseResult.success) {
        logWarn(
          `SAML profile parsing failed: ${parseResult.error.message}`,
          undefined,
          {
            issuer: profile.issuer
          },
          parseResult.error
        )
      }
      verify(profile)
        .then((user) => {
          // Despite what the typings say, passport-saml assumes
          // we give it back a valid Profile, including at least some of these
          // SAML-specific fields
          const samlUser: EvakaSessionUser & Profile = {
            ...user,
            issuer: profile.issuer,
            nameID: profile.nameID,
            nameIDFormat: profile.nameIDFormat,
            nameQualifier: profile.nameQualifier,
            spNameQualifier: profile.spNameQualifier,
            sessionIndex: profile.sessionIndex
          }
          done(null, samlUser)
        })
        .catch(done)
    }
  }
}
