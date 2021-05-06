// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import {
  Profile,
  Strategy as SamlStrategy,
  VerifiedCallback
} from 'passport-saml'
import DevPassportStrategy from './dev-passport-strategy'
import { SamlUser } from '../routes/auth/saml/types'
import { devLoginEnabled, adConfig, adExternalIdPrefix } from '../config'
import certificates from '../certificates'
import { readFileSync } from 'fs'
import { upsertEmployee } from '../dev-api'
import { getOrCreateEmployee, UserRole } from '../service-client'
import { RedisClient } from 'redis'
import redisCacheProvider from './passport-saml-cache-redis'

const AD_USER_ID_KEY =
  'http://schemas.microsoft.com/identity/claims/objectidentifier'
const AD_ROLES_KEY =
  'http://schemas.microsoft.com/ws/2008/06/identity/claims/role'
const AD_GIVEN_NAME_KEY =
  'http://schemas.xmlsoap.org/ws/2005/05/identity/claims/givenname'
const AD_FAMILY_NAME_KEY =
  'http://schemas.xmlsoap.org/ws/2005/05/identity/claims/surname'
const AD_EMAIL_KEY =
  'http://schemas.xmlsoap.org/ws/2005/05/identity/claims/emailaddress'

interface AdProfile {
  nameID?: Profile['nameID']
  nameIDFormat?: Profile['nameIDFormat']
  nameQualifier?: Profile['nameQualifier']
  spNameQualifier?: Profile['spNameQualifier']
  sessionIndex?: Profile['sessionIndex']
  [AD_USER_ID_KEY]: string
  [AD_ROLES_KEY]: string | string[]
  [AD_GIVEN_NAME_KEY]: string
  [AD_FAMILY_NAME_KEY]: string
  [AD_EMAIL_KEY]: string
}

async function verifyProfile(profile: AdProfile): Promise<SamlUser> {
  const aad = profile[AD_USER_ID_KEY]
  if (!aad) throw Error('No user ID in SAML data')
  const person = await getOrCreateEmployee({
    externalId: `${adExternalIdPrefix}:${aad}`,
    firstName: profile[AD_GIVEN_NAME_KEY],
    lastName: profile[AD_FAMILY_NAME_KEY],
    email: profile[AD_EMAIL_KEY]
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

export default function createAdStrategy(
  redisClient?: RedisClient
): SamlStrategy | DevPassportStrategy {
  if (devLoginEnabled) {
    const getter = async (userId: string) =>
      verifyProfile({
        [AD_USER_ID_KEY]: userId,
        [AD_ROLES_KEY]: [],
        [AD_GIVEN_NAME_KEY]: '',
        [AD_FAMILY_NAME_KEY]: '',
        [AD_EMAIL_KEY]: ''
      })

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
        externalId: `${adExternalIdPrefix}:${userId}`,
        roles: roles as UserRole[]
      })
      return verifyProfile({
        [AD_USER_ID_KEY]: userId,
        [AD_ROLES_KEY]: roles,
        [AD_GIVEN_NAME_KEY]: firstName,
        [AD_FAMILY_NAME_KEY]: lastName,
        [AD_EMAIL_KEY]: email
      })
    }

    return new DevPassportStrategy(getter, upserter)
  } else {
    if (!adConfig) throw Error('Missing AD SAML configuration')
    return new SamlStrategy(
      {
        acceptedClockSkewMs: 0,
        audience: adConfig.issuer,
        cacheProvider: redisClient
          ? redisCacheProvider(redisClient, {
              keyPrefix: 'ad-saml-resp:'
            })
          : undefined,
        callbackUrl: adConfig.callbackUrl,
        cert: adConfig.publicCert.map(
          (certificateName) => certificates[certificateName]
        ),
        disableRequestedAuthnContext: true,
        entryPoint: adConfig.entryPointUrl,
        identifierFormat: 'urn:oasis:names:tc:SAML:2.0:nameid-format:transient',
        issuer: adConfig.issuer,
        logoutUrl: adConfig.logoutUrl,
        privateCert: readFileSync(adConfig.privateCert, { encoding: 'utf8' }),
        signatureAlgorithm: 'sha256',
        validateInResponseTo: true
      },
      (profile: Profile, done: VerifiedCallback) => {
        // eslint-disable-next-line @typescript-eslint/no-explicit-any
        verifyProfile((profile as any) as AdProfile)
          .then((user) => done(null, user))
          .catch(done)
      }
    )
  }
}
