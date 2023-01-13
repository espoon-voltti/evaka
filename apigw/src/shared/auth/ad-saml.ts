// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import passportSaml, {
  Profile,
  SamlConfig,
  Strategy as SamlStrategy,
  VerifiedCallback
} from 'passport-saml'
import DevAdStrategy from './dev-ad-strategy'
import { SamlUser } from '../routes/auth/saml/types'
import certificates from '../certificates'
import { readFileSync } from 'fs'
import { getEmployeeByExternalId, upsertEmployee } from '../dev-api'
import { employeeLogin, UserRole } from '../service-client'
import { RedisClient } from 'redis'
import redisCacheProvider from './passport-saml-cache-redis'
import { Config } from '../config'

const AD_OID_KEY =
  'http://schemas.microsoft.com/identity/claims/objectidentifier'
const AD_GIVEN_NAME_KEY =
  'http://schemas.xmlsoap.org/ws/2005/05/identity/claims/givenname'
const AD_FAMILY_NAME_KEY =
  'http://schemas.xmlsoap.org/ws/2005/05/identity/claims/surname'
const AD_EMAIL_KEY =
  'http://schemas.xmlsoap.org/ws/2005/05/identity/claims/emailaddress'
const AD_EMPLOYEE_NUMBER_KEY =
  'http://schemas.xmlsoap.org/ws/2005/05/identity/claims/employeenumber'

async function verifyProfile(
  idPrefix: string,
  profile: passportSaml.Profile
): Promise<SamlUser> {
  const asString = (value: unknown) =>
    value == null ? undefined : String(value)

  const aad = asString(profile[AD_OID_KEY])
  if (!aad) throw Error('No user ID in SAML data')
  const person = await employeeLogin({
    externalId: `${idPrefix}:${aad}`,
    firstName: asString(profile[AD_GIVEN_NAME_KEY]) ?? '',
    lastName: asString(profile[AD_FAMILY_NAME_KEY]) ?? '',
    email: asString(profile[AD_EMAIL_KEY]),
    employeeNumber: asString(profile[AD_EMPLOYEE_NUMBER_KEY])
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

export function createSamlConfig(
  config: Config['ad'],
  redisClient?: RedisClient
): SamlConfig {
  if (config.mock) return { cert: 'mock-certificate' }
  if (!config.saml) throw Error('Missing AD SAML configuration')
  return {
    acceptedClockSkewMs: 0,
    audience: config.saml.issuer,
    cacheProvider: redisClient
      ? redisCacheProvider(redisClient, { keyPrefix: 'ad-saml-resp:' })
      : undefined,
    callbackUrl: config.saml.callbackUrl,
    cert: Array.isArray(config.saml.publicCert)
      ? config.saml.publicCert.map(
          (certificateName) => certificates[certificateName]
        )
      : config.saml.publicCert,
    disableRequestedAuthnContext: true,
    entryPoint: config.saml.entryPoint,
    identifierFormat: 'urn:oasis:names:tc:SAML:2.0:nameid-format:transient',
    issuer: config.saml.issuer,
    logoutUrl: config.saml.logoutUrl,
    privateKey: readFileSync(config.saml.privateCert, { encoding: 'utf8' }),
    signatureAlgorithm: 'sha256',
    validateInResponseTo: config.saml.validateInResponseTo
  }
}

export default function createAdStrategy(
  config: Config['ad'],
  samlConfig: SamlConfig
): SamlStrategy | DevAdStrategy {
  if (config.mock) {
    const getter = async (userId: string) => {
      const employee = await getEmployeeByExternalId(
        `${config.externalIdPrefix}:${userId}`
      )
      return verifyProfile(config.externalIdPrefix, {
        nameID: 'dummyid',
        [AD_OID_KEY]: userId,
        [AD_GIVEN_NAME_KEY]: employee.firstName,
        [AD_FAMILY_NAME_KEY]: employee.lastName,
        [AD_EMAIL_KEY]: employee.email ? employee.email : ''
      })
    }

    const upserter = async (
      userId: string,
      roles: string[],
      firstName: string,
      lastName: string,
      email: string
    ) => {
      if (!userId) throw Error('No user ID in SAML data')
      await upsertEmployee({
        firstName,
        lastName,
        email,
        externalId: `${config.externalIdPrefix}:${userId}`,
        roles: roles as UserRole[]
      })
      return verifyProfile(config.externalIdPrefix, {
        nameID: 'dummyid',
        [AD_OID_KEY]: userId,
        [AD_GIVEN_NAME_KEY]: firstName,
        [AD_FAMILY_NAME_KEY]: lastName,
        [AD_EMAIL_KEY]: email
      })
    }

    return new DevAdStrategy(getter, upserter)
  } else {
    return new SamlStrategy(
      samlConfig,
      (profile: Profile | null | undefined, done: VerifiedCallback) => {
        if (!profile) {
          done(new Error('No SAML profile'))
        } else {
          verifyProfile(config.externalIdPrefix, profile)
            .then((user) => done(null, user))
            .catch(done)
        }
      }
    )
  }
}
