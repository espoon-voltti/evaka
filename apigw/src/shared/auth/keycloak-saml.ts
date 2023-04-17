// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import passportSaml, {
  Profile,
  SamlConfig,
  Strategy as SamlStrategy,
  VerifiedCallback
} from 'passport-saml'
import { SamlUser } from '../routes/auth/saml/types'
import { employeeLogin } from '../service-client'
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
    privateKey: privateCert,
    signatureAlgorithm: 'sha256',
    validateInResponseTo: evakaSamlConfig.validateInResponseTo
  }
}

export default function createKeycloakSamlStrategy(
  config: SamlConfig
): SamlStrategy {
  return new SamlStrategy(
    config,
    (profile: Profile | null | undefined, done: VerifiedCallback) => {
      if (!profile) {
        done(new Error('No SAML profile'))
      } else {
        verifyKeycloakProfile(profile)
          .then((user) => done(null, user))
          .catch(done)
      }
    }
  )
}

async function verifyKeycloakProfile(
  profile: passportSaml.Profile
): Promise<SamlUser> {
  const asString = (value: unknown) =>
    value == null ? undefined : String(value)

  const id = asString(profile['id'])
  if (!id) throw Error('No user ID in evaka IDP SAML data')
  const person = await employeeLogin({
    externalId: `evaka:${id}`,
    firstName: asString(profile['firstName']) ?? '',
    lastName: asString(profile['lastName']) ?? '',
    email: asString(profile['email'])
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
