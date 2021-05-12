// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import fs from 'fs'
import {
  Profile,
  SamlConfig,
  Strategy as SamlStrategy,
  VerifiedCallback
} from 'passport-saml'
import { RedisClient } from 'redis'
import { evakaCustomerSamlConfig } from '../config'
import { SamlUser } from '../routes/auth/saml/types'
import { getOrCreatePerson } from '../service-client'
import redisCacheProvider from './passport-saml-cache-redis'

export function createSamlConfig(redisClient?: RedisClient): SamlConfig {
  if (!evakaCustomerSamlConfig)
    throw new Error('Missing Keycloak SAML configuration')
  if (Array.isArray(evakaCustomerSamlConfig.publicCert))
    throw new Error('Expected a single string as publicCert')
  const publicCert = fs.readFileSync(evakaCustomerSamlConfig.publicCert, {
    encoding: 'utf8'
  })
  const privateCert = fs.readFileSync(evakaCustomerSamlConfig.privateCert, {
    encoding: 'utf8'
  })
  return {
    acceptedClockSkewMs: -1,
    cacheProvider: redisClient
      ? redisCacheProvider(redisClient, {
          keyPrefix: 'customer-saml-resp:'
        })
      : undefined,
    callbackUrl: evakaCustomerSamlConfig.callbackUrl,
    cert: publicCert,
    decryptionPvk: privateCert,
    entryPoint: evakaCustomerSamlConfig.entryPoint,
    identifierFormat: 'urn:oasis:names:tc:SAML:2.0:nameid-format:transient',
    issuer: evakaCustomerSamlConfig.issuer,
    logoutUrl: evakaCustomerSamlConfig.entryPoint,
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
      verifyKeycloakProfile((profile as any) as KeycloakProfile)
        .then((user) => done(null, user))
        .catch(done)
    }
  )
}

interface KeycloakProfile {
  id?: string
  socialSecurityNumber: string
  email: string
  firstName: string
  lastName: string
  nameID?: string
  nameIDFormat?: string
  nameQualifier?: string
  spNameQualifier?: string
  sessionIndex?: string
}

async function verifyKeycloakProfile(
  profile: KeycloakProfile
): Promise<SamlUser> {
  if (!profile.socialSecurityNumber)
    throw Error('No socialSecurityNumber in evaka IDP SAML data')

  const person = await getOrCreatePerson({
    socialSecurityNumber: profile.socialSecurityNumber,
    firstName: profile.firstName,
    lastName: profile.lastName
  })

  return {
    id: person.id,
    userType: 'CITIZEN_WEAK',
    globalRoles: ['CITIZEN_WEAK'],
    allScopedRoles: [],
    nameID: profile.nameID,
    nameIDFormat: profile.nameIDFormat,
    nameQualifier: profile.nameQualifier,
    spNameQualifier: profile.spNameQualifier,
    sessionIndex: profile.sessionIndex
  }
}
