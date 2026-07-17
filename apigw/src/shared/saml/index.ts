// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { readFileSync } from 'node:fs'

import type { CacheProvider, Profile, SamlConfig } from '@node-saml/node-saml'
import type express from 'express'
import { z } from 'zod'

import type { EvakaSessionUser } from '../auth/index.ts'
import type { TrustedCertificates } from '../certificates.ts'
import certificates from '../certificates.ts'
import type { EvakaSamlConfig } from '../config.ts'
import { evakaBaseUrl } from '../config.ts'
import { logError } from '../logging.ts'
import { parseUrlWithOrigin } from '../parse-url-with-origin.ts'

export function createSamlConfig(
  config: EvakaSamlConfig,
  cacheProvider?: CacheProvider,
  wantAuthnResponseSigned = true
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
    acceptedClockSkewMs: config.acceptedClockSkewMs,
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
    wantAssertionsSigned: true,
    wantAuthnResponseSigned
  }
}

export type AuthenticateProfile = (
  req: express.Request,
  profile: Profile
) => Promise<EvakaSessionUser>

export function authenticateProfile<T>(
  schema: z.ZodType<T>,
  authenticate: (
    req: express.Request,
    samlSession: SamlSession,
    profile: T
  ) => Promise<EvakaSessionUser>
): AuthenticateProfile {
  return async (req, profile) => {
    const samlSession = SamlSessionSchema.parse(profile)
    const parseResult = schema.safeParse(profile)
    if (parseResult.success) {
      return await authenticate(req, samlSession, parseResult.data)
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

export type SamlSession = z.infer<typeof SamlSessionSchema>

// A subset of SAML Profile fields that are expected to be present in valid SAML sessions
export const SamlSessionSchema = z.object({
  issuer: z.string(),
  nameID: z.string(),
  nameIDFormat: z.string(),
  sessionIndex: z.string().optional(),
  nameQualifier: z.string().optional(),
  spNameQualifier: z.string().optional()
})

const CORRELATION_TOKEN_PARAM = '&sfiCorr='

export function getRawUnvalidatedRelayState(
  req: express.Request
): string | undefined {
  // oxlint-disable-next-line typescript/no-unsafe-member-access,typescript/no-unsafe-assignment
  const relayState = req.body?.RelayState || req.query.RelayState
  if (typeof relayState !== 'string') return undefined
  return relayState
}

export function buildRelayStateWithCorrelationToken(
  relayState: string,
  token: string | undefined
): string {
  return token
    ? `${relayState}${CORRELATION_TOKEN_PARAM}${encodeURIComponent(token)}`
    : relayState
}

export function extractCorrelationToken(
  req: express.Request
): string | undefined {
  const relayState = getRawUnvalidatedRelayState(req)
  if (!relayState) return undefined
  const start = relayState.lastIndexOf(CORRELATION_TOKEN_PARAM)
  if (start < 0) return undefined
  const encoded = relayState.slice(start + CORRELATION_TOKEN_PARAM.length)
  if (!encoded) return undefined
  try {
    return decodeURIComponent(encoded)
  } catch (err) {
    return undefined
  }
}

// SAML RelayState is an arbitrary string that gets passed in a SAML transaction.
// In our case, we specify it to be a redirect URL where the user should be
// redirected to after the SAML transaction is complete. Since the RelayState
// is not signed or encrypted, we must make sure the URL points to our application
// and not to some 3rd party domain
export function validateRelayStateUrl(req: express.Request): URL | undefined {
  const raw = getRawUnvalidatedRelayState(req)
  if (raw) {
    const i = raw.lastIndexOf(CORRELATION_TOKEN_PARAM)
    const relayState = i >= 0 ? raw.slice(0, i) : raw
    const url = parseUrlWithOrigin(evakaBaseUrl, relayState)
    if (url) return url
    logError('Invalid RelayState in request', req)
  }
  return undefined
}
