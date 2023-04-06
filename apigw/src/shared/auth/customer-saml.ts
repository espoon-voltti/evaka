// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import fs from 'fs'
import passportSaml, {
  Profile,
  SamlConfig,
  Strategy as SamlStrategy,
  VerifiedCallback
} from '@node-saml/passport-saml'
import { RedisClient } from 'redis'
import { evakaCustomerSamlConfig } from '../config'
import { EvakaUserFields } from '../routes/auth/saml/types'
import { citizenLogin } from '../service-client'
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
    privateKey: privateCert,
    signatureAlgorithm: 'sha256',
    validateInResponseTo: evakaCustomerSamlConfig.validateInResponseTo
  }
}

export default function createKeycloakSamlStrategy(
  config: SamlConfig
): SamlStrategy {
  return new SamlStrategy(
    config,
    (profile: Profile | null, done: VerifiedCallback) => {
      if (!profile) {
        done(new Error('No SAML profile'))
      } else {
        verifyKeycloakProfile(profile)
          .then((user) => done(null, user))
          .catch(done)
      }
    },
    (profile, done) => done(null)
  )
}

async function verifyKeycloakProfile(
  profile: passportSaml.Profile
): Promise<passportSaml.Profile & EvakaUserFields> {
  const asString = (value: unknown) =>
    value == null ? undefined : String(value)

  const socialSecurityNumber = asString(profile['socialSecurityNumber'])
  if (!socialSecurityNumber)
    throw Error('No socialSecurityNumber in evaka IDP SAML data')

  const person = await citizenLogin({
    socialSecurityNumber: socialSecurityNumber,
    firstName: asString(profile['firstName']) ?? '',
    lastName: asString(profile['lastName']) ?? ''
  })

  return {
    id: person.id,
    userType: 'CITIZEN_WEAK',
    globalRoles: ['CITIZEN_WEAK'],
    allScopedRoles: [],
    issuer: profile.issuer,
    nameID: profile.nameID,
    nameIDFormat: profile.nameIDFormat,
    nameQualifier: profile.nameQualifier,
    spNameQualifier: profile.spNameQualifier,
    sessionIndex: profile.sessionIndex
  }
}
