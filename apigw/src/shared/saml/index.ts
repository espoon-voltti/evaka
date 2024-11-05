// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { readFileSync } from 'node:fs'
import path from 'node:path'

import { CacheProvider, Profile, SamlConfig } from '@node-saml/node-saml'
import express from 'express'
import { z } from 'zod'

import { EvakaSessionUser } from '../auth/index.js'
import certificates, { TrustedCertificates } from '../certificates.js'
import { evakaBaseUrl, EvakaSamlConfig } from '../config.js'
import { logError } from '../logging.js'

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
    idpCert: publicCert,
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
    validateInResponseTo: config.validateInResponseTo,
    // When *both* wantXXXXSigned settings are false, node-saml still
    // requires at least the whole response *or* the assertion to be signed, so
    // these settings don't introduce a security problem
    wantAssertionsSigned: false,
    wantAuthnResponseSigned: false
  }
}

export type AuthenticateProfile = (
  profile: Profile
) => Promise<EvakaSessionUser>

export function authenticateProfile<T>(
  schema: z.ZodType<T>,
  authenticate: (profile: T) => Promise<EvakaSessionUser>
): AuthenticateProfile {
  return async (profile) => {
    const parseResult = schema.safeParse(profile)
    if (parseResult.success) {
      return await authenticate(parseResult.data)
    } else {
      throw new Error(
        `SAML ${profile.issuer} profile parsing failed: ${parseResult.error.message}`
      )
    }
  }
}

export const SamlProfileIdSchema = z.object({
  nameID: z.string(),
  nameIDFormat: z.string()
})

// A subset of SAML Profile fields that are expected to be present in Profile
// *and* req.user in valid SAML sessions
export const SamlProfileSchema = z.object({
  issuer: z.string(),
  nameID: z.string(),
  nameIDFormat: z.string(),
  sessionIndex: z.string().optional(),
  nameQualifier: z.string().optional(),
  spNameQualifier: z.string().optional()
})

export function parseRelayState(req: express.Request): string | undefined {
  // eslint-disable-next-line @typescript-eslint/no-unsafe-member-access,@typescript-eslint/no-unsafe-assignment
  const relayState = req.body?.RelayState || req.query.RelayState

  if (typeof relayState === 'string' && path.isAbsolute(relayState)) {
    if (evakaBaseUrl === 'local') {
      return relayState
    } else {
      const baseUrl = evakaBaseUrl.replace(/\/$/, '')
      const redirect = new URL(relayState, baseUrl)
      if (redirect.origin == baseUrl) {
        return redirect.href
      }
    }
  }

  if (relayState) logError('Invalid RelayState in request', req)

  return undefined
}
