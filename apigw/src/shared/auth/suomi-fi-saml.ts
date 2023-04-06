// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import fs from 'fs'
import passportSaml, {
  Profile,
  SamlConfig,
  Strategy,
  VerifiedCallback
} from '@node-saml/passport-saml'
import { RedisClient } from 'redis'
import certificates from '../certificates'
import { Config } from '../config'
import { EvakaUserFields } from '../routes/auth/saml/types'
import { citizenLogin } from '../service-client'
import redisCacheProvider from './passport-saml-cache-redis'
import { getCitizenBySsn } from '../dev-api'
import DevSfiStrategy from './dev-sfi-strategy'

// Suomi.fi e-Identification – Attributes transmitted on an identified user:
//   https://esuomi.fi/suomi-fi-services/suomi-fi-e-identification/14247-2/?lang=en
// Note: Suomi.fi only returns the values we request in our SAML metadata
const SUOMI_FI_SSN_KEY = 'urn:oid:1.2.246.21'
const SUOMI_FI_GIVEN_NAME_KEY = 'urn:oid:2.5.4.42'
const SUOMI_FI_SURNAME_KEY = 'urn:oid:2.5.4.4'

async function verifyProfile(
  profile: passportSaml.Profile
): Promise<passportSaml.Profile & EvakaUserFields> {
  const asString = (value: unknown) =>
    value == null ? undefined : String(value)

  const socialSecurityNumber = asString(profile[SUOMI_FI_SSN_KEY])
  if (!socialSecurityNumber) throw Error('No SSN in SAML data')
  const person = await citizenLogin({
    socialSecurityNumber,
    firstName: asString(profile[SUOMI_FI_GIVEN_NAME_KEY]) ?? '',
    lastName: asString(profile[SUOMI_FI_SURNAME_KEY]) ?? ''
  })
  return {
    id: person.id,
    userType: 'ENDUSER',
    globalRoles: ['END_USER'],
    allScopedRoles: [],
    issuer: profile.issuer,
    nameID: profile.nameID,
    nameIDFormat: profile.nameIDFormat,
    nameQualifier: profile.nameQualifier,
    spNameQualifier: profile.spNameQualifier,
    sessionIndex: profile.sessionIndex
  }
}

export function createSamlConfig(
  config: Config['sfi'],
  redisClient?: RedisClient
): SamlConfig {
  if (config.mock) return { cert: 'mock-certificate', issuer: 'mock' }
  if (!config.saml) throw new Error('Missing Suomi.fi SAML configuration')
  const publicCert = Array.isArray(config.saml.publicCert)
    ? config.saml.publicCert.map(
        (certificateName) => certificates[certificateName]
      )
    : fs.readFileSync(config.saml.publicCert, {
        encoding: 'utf8'
      })
  const privateCert = fs.readFileSync(config.saml.privateCert, {
    encoding: 'utf8'
  })

  return {
    acceptedClockSkewMs: 0,
    audience: config.saml.issuer,
    cacheProvider: redisClient
      ? redisCacheProvider(redisClient, { keyPrefix: 'suomifi-saml-resp:' })
      : undefined,
    callbackUrl: config.saml.callbackUrl,
    cert: publicCert,
    decryptionPvk: privateCert,
    disableRequestedAuthnContext: true,
    entryPoint: config.saml.entryPoint,
    identifierFormat: 'urn:oasis:names:tc:SAML:2.0:nameid-format:transient',
    issuer: config.saml.issuer,
    logoutUrl: config.saml.logoutUrl,
    privateKey: privateCert,
    signatureAlgorithm: 'sha256',
    validateInResponseTo: config.saml.validateInResponseTo
  }
}

export default function createSuomiFiStrategy(
  config: Config['sfi'],
  samlConfig: SamlConfig
): Strategy | DevSfiStrategy {
  if (config.mock) {
    const getter = async (ssn: string) => {
      const citizen = await getCitizenBySsn(ssn)
      return verifyProfile({
        issuer: 'mock',
        nameID: 'dummyid',
        nameIDFormat: 'dummyformat',
        [SUOMI_FI_SSN_KEY]: citizen.ssn,
        [SUOMI_FI_GIVEN_NAME_KEY]: citizen.firstName,
        [SUOMI_FI_SURNAME_KEY]: citizen.lastName
      })
    }

    return new DevSfiStrategy(getter)
  } else {
    return new Strategy(
      samlConfig,
      (profile: Profile | null, done: VerifiedCallback) => {
        if (!profile) {
          done(new Error('No SAML profile'))
        } else {
          verifyProfile(profile)
            .then((user) => done(null, user))
            .catch(done)
        }
      },
      (profile, done) => done(null)
    )
  }
}
