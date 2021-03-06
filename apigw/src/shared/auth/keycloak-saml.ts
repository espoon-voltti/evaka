// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import {
  Profile,
  SamlConfig,
  Strategy as SamlStrategy,
  VerifiedCallback
} from 'passport-saml'
import { SamlUser } from '../routes/auth/saml/types'
import { getOrCreateEmployee } from '../service-client'
import { evakaSamlConfig } from '../config'
import fs from 'fs'
import { RedisClient } from 'redis'
import redisCacheProvider from './passport-saml-cache-redis'

export function createSamlConfig(redisClient?: RedisClient): SamlConfig {
  if (!evakaSamlConfig) throw new Error('Missing Keycloak SAML configuration')
  if (Array.isArray(evakaSamlConfig.publicCert))
    throw new Error('Expected a single string as publicCert')
  const publicCert = fs.readFileSync(evakaSamlConfig.publicCert, {
    encoding: 'utf8'
  })
  const privateCert = fs.readFileSync(evakaSamlConfig.privateCert, {
    encoding: 'utf8'
  })
  return {
    acceptedClockSkewMs: 0,
    audience: evakaSamlConfig.issuer,
    cacheProvider: redisClient
      ? redisCacheProvider(redisClient, { keyPrefix: 'keycloak-saml-resp:' })
      : undefined,
    callbackUrl: evakaSamlConfig.callbackUrl,
    cert: publicCert,
    decryptionPvk: privateCert,
    entryPoint: evakaSamlConfig.entryPoint,
    identifierFormat: 'urn:oasis:names:tc:SAML:2.0:nameid-format:transient',
    issuer: evakaSamlConfig.issuer,
    logoutUrl: evakaSamlConfig.entryPoint,
    privateCert: privateCert,
    signatureAlgorithm: 'sha256',
    validateInResponseTo: true
  }
}

export default function createKeycloakSamlStrategy(
  config: SamlConfig
): SamlStrategy {
  return new SamlStrategy(
    config,
    (profile: Profile, done: VerifiedCallback) => {
      // eslint-disable-next-line @typescript-eslint/no-explicit-any
      verifyKeycloakProfile(profile as KeycloakProfile)
        .then((user) => done(null, user))
        .catch(done)
    }
  )
}

interface KeycloakProfile {
  id?: string
  email?: string
  firstName?: string
  lastName?: string
  nameID?: string
  nameIDFormat?: string
  nameQualifier?: string
  spNameQualifier?: string
  sessionIndex?: string
}

async function verifyKeycloakProfile(
  profile: KeycloakProfile
): Promise<SamlUser> {
  if (!profile.id) throw Error('No user ID in evaka IDP SAML data')
  const person = await getOrCreateEmployee({
    externalId: `evaka:${profile.id}`,
    firstName: profile.firstName ?? '',
    lastName: profile.lastName ?? '',
    email: profile.email
  })
  return {
    id: person.id,
    userType: 'EMPLOYEE',
    globalRoles: person.globalRoles,
    allScopedRoles: person.allScopedRoles,
    nameID: profile.nameID,
    nameIDFormat: profile.nameIDFormat,
    nameQualifier: profile.nameQualifier,
    spNameQualifier: profile.spNameQualifier,
    sessionIndex: profile.sessionIndex
  }
}
