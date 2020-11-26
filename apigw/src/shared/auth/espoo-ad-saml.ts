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
import {
  eadMock,
  eadSamlCallbackUrl,
  eadSamlIssuer,
  eadSamlPrivateCert,
  eadSamlPublicCert
} from '../config'
import certificates from '../certificates'
import { readFileSync } from 'fs'
import { upsertEmployee } from '../dev-api'
import { getOrCreateEmployee, UserRole } from '../service-client'

const ESPOO_AD_USER_ID_KEY =
  'http://schemas.microsoft.com/identity/claims/objectidentifier'
const ESPOO_AD_ROLES_KEY =
  'http://schemas.microsoft.com/ws/2008/06/identity/claims/role'
const ESPOO_AD_GIVEN_NAME_KEY =
  'http://schemas.xmlsoap.org/ws/2005/05/identity/claims/givenname'
const ESPOO_AD_FAMILY_NAME_KEY =
  'http://schemas.xmlsoap.org/ws/2005/05/identity/claims/surname'
const ESPOO_AD_EMAIL_KEY =
  'http://schemas.xmlsoap.org/ws/2005/05/identity/claims/emailaddress'

interface EspooAdProfile {
  nameID?: Profile['nameID']
  nameIDFormat?: Profile['nameIDFormat']
  nameQualifier?: Profile['nameQualifier']
  spNameQualifier?: Profile['spNameQualifier']
  sessionIndex?: Profile['sessionIndex']
  [ESPOO_AD_USER_ID_KEY]: string
  [ESPOO_AD_ROLES_KEY]: string | string[]
  [ESPOO_AD_GIVEN_NAME_KEY]: string
  [ESPOO_AD_FAMILY_NAME_KEY]: string
  [ESPOO_AD_EMAIL_KEY]: string
}

async function verifyProfile(profile: EspooAdProfile): Promise<SamlUser> {
  const person = await getOrCreateEmployee({
    aad: profile[ESPOO_AD_USER_ID_KEY],
    firstName: profile[ESPOO_AD_GIVEN_NAME_KEY],
    lastName: profile[ESPOO_AD_FAMILY_NAME_KEY],
    email: profile[ESPOO_AD_EMAIL_KEY]
  })
  return {
    id: person.id,
    roles: person.roles,
    nameID: profile.nameID,
    nameIDFormat: profile.nameIDFormat,
    nameQualifier: profile.nameQualifier,
    spNameQualifier: profile.spNameQualifier,
    sessionIndex: profile.sessionIndex
  }
}

export default function createEspooAdStrategy():
  | SamlStrategy
  | DevPassportStrategy {
  if (eadMock) {
    const getter = async (userId: string) =>
      verifyProfile({
        [ESPOO_AD_USER_ID_KEY]: userId,
        [ESPOO_AD_ROLES_KEY]: [],
        [ESPOO_AD_GIVEN_NAME_KEY]: '',
        [ESPOO_AD_FAMILY_NAME_KEY]: '',
        [ESPOO_AD_EMAIL_KEY]: ''
      })

    const upserter = async (
      userId: string,
      roles: string[],
      firstName: string,
      lastName: string,
      email: string
    ) => {
      await upsertEmployee({
        firstName,
        lastName,
        email,
        aad: userId,
        roles: roles as UserRole[]
      })
      return verifyProfile({
        [ESPOO_AD_USER_ID_KEY]: userId,
        [ESPOO_AD_ROLES_KEY]: roles,
        [ESPOO_AD_GIVEN_NAME_KEY]: firstName,
        [ESPOO_AD_FAMILY_NAME_KEY]: lastName,
        [ESPOO_AD_EMAIL_KEY]: email
      })
    }

    return new DevPassportStrategy(getter, upserter)
  } else {
    if (!eadSamlPublicCert)
      throw new Error('No Espoo AD SAML public certificate configured')
    if (!eadSamlPrivateCert)
      throw new Error('No Espoo AD SAML private certificate configured')
    return new SamlStrategy(
      {
        callbackUrl: eadSamlCallbackUrl,
        entryPoint:
          'https://login.microsoftonline.com/6bb04228-cfa5-4213-9f39-172454d82584/saml2',
        logoutUrl:
          'https://login.microsoftonline.com/6bb04228-cfa5-4213-9f39-172454d82584/saml2',
        issuer: eadSamlIssuer,
        cert: eadSamlPublicCert.map(
          (certificateName) => certificates[certificateName]
        ),
        privateCert: readFileSync(eadSamlPrivateCert, {
          encoding: 'utf8'
        }),
        identifierFormat: 'urn:oasis:names:tc:SAML:2.0:nameid-format:transient',
        disableRequestedAuthnContext: true,
        signatureAlgorithm: 'sha256',
        acceptedClockSkewMs: -1
      },
      (profile: Profile, done: VerifiedCallback) => {
        // eslint-disable-next-line @typescript-eslint/no-explicit-any
        verifyProfile((profile as any) as EspooAdProfile)
          .then((user) => done(null, user))
          .catch(done)
      }
    )
  }
}
