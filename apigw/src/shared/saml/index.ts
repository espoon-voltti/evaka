// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { z } from 'zod'
import { isEqual } from 'lodash'
import {
  CacheProvider,
  Profile,
  SamlConfig,
  Strategy as SamlStrategy,
  VerifyWithRequest
} from '@node-saml/passport-saml'
import { logError, logWarn } from '../logging'
import { EvakaSessionUser } from '../auth'
import { evakaBaseUrl, EvakaSamlConfig } from '../config'
import { readFileSync } from 'fs'
import certificates, { TrustedCertificates } from '../certificates'
import express from 'express'
import path from 'path'

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

// A subset of SAML Profile fields that are expected to be present in Profile
// *and* req.user in valid SAML sessions
function samlProfileId({
  issuer,
  sessionIndex,
  nameID,
  nameIDFormat
}: Profile) {
  return { issuer, sessionIndex, nameID, nameIDFormat }
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
    (async () => {
      if (!profile) return undefined
      if (req.user) {
        const reqUser = req.user as Profile
        const reqId = samlProfileId(reqUser)
        const profileId = samlProfileId(profile)
        if (isEqual(reqId, profileId)) {
          return reqUser
        }
      }
    })()
      .then((user) => done(null, user))
      .catch((err) => done(err))
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
