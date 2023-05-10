// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { z } from 'zod'
import {
  CacheProvider,
  Profile,
  SAML,
  SamlConfig,
  VerifyWithRequest
} from '@node-saml/passport-saml'
import { logError, logWarn } from '../logging'
import { EvakaSessionUser } from '../auth'
import { evakaBaseUrl, EvakaSamlConfig } from '../config'
import { readFileSync } from 'fs'
import certificates, { TrustedCertificates } from '../certificates'
import express, { Request } from 'express'
import path from 'path'
import { Strategy as SamlStrategy } from '@node-saml/passport-saml/lib/strategy'

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
    validateInResponseTo: config.validateInResponseTo,
    wantAssertionsSigned: false,
    passReqToCallback: true
  }
}

export function createSamlStrategy(
  config: SamlConfig,
  profileSchema: z.AnyZodObject,
  login: (profile: Profile) => Promise<EvakaSessionUser>
): SamlStrategy {
  const loginVerify: VerifyWithRequest = (req, profile, done) => {
    if (!profile) return done(null, undefined)
    const parseResult = profileSchema.safeParse(profile)
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
    login(profile)
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
  const logoutVerify: VerifyWithRequest = (req, profile, done) =>
    done(null, profile ?? undefined)
  return new SamlStrategy(config, loginVerify, logoutVerify)
}

export function parseRelayState(req: express.Request): string | undefined {
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

/**
 * If request is a SAMLRequest, parse, validate and return the Profile from it.
 * @param saml Config must match active strategy's config
 */
export async function tryParseProfile(
  req: Request,
  saml: SAML
): Promise<Profile | undefined> {
  let profile: Profile | null | undefined

  // NOTE: This duplicate parsing can be removed if passport-saml ever exposes
  // an alternative for passport.authenticate() that either lets us hook into
  // it before any redirects or separate XML parsing and authentication methods.
  if (req.query?.SAMLRequest) {
    // Redirects have signatures in the original query parameter
    const dummyOrigin = 'http://evaka'
    const originalQuery = new URL(req.url, dummyOrigin).search.replace(
      /^\?/,
      ''
    )
    profile = (await saml.validateRedirectAsync(req.query, originalQuery))
      .profile
  } else if (req.body?.SAMLRequest) {
    // POST logout callbacks have the signature in the message body directly
    profile = (await saml.validatePostRequestAsync(req.body)).profile
  }

  return profile || undefined
}
