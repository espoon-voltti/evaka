// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import {
  Profile,
  Strategy as SamlStrategy,
  VerifiedCallback
} from 'passport-saml'
import { SamlUser } from '../routes/auth/saml/types'
import { getOrCreatePerson } from '../service-client'
import { evakaCustomerSamlConfig, EvakaSamlConfig } from '../config'
import fs from 'fs'

export default function createEvakaCustomerSamlStrategy(): SamlStrategy {
  return createKeycloakSamlStrategy(evakaCustomerSamlConfig)
}

function createKeycloakSamlStrategy(
  samlConfig: EvakaSamlConfig | undefined
): SamlStrategy {
  if (!samlConfig) throw new Error('Missing Keycloak SAML configuration')
  const publicCert = fs.readFileSync(samlConfig.publicCert, {
    encoding: 'utf8'
  })
  const privateCert = fs.readFileSync(samlConfig.privateCert, {
    encoding: 'utf8'
  })
  return new SamlStrategy(
    {
      issuer: 'evaka-customer',
      callbackUrl: samlConfig.callbackUrl,
      entryPoint: samlConfig.entryPoint,
      logoutUrl: samlConfig.entryPoint,
      acceptedClockSkewMs: -1,
      cert: publicCert,
      privateCert: privateCert,
      decryptionPvk: privateCert,
      identifierFormat: 'urn:oasis:names:tc:SAML:2.0:nameid-format:transient',
      signatureAlgorithm: 'sha256'
    },
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
    userType: 'ENDUSER',
    globalRoles: ['END_USER'],
    allScopedRoles: [],
    nameID: profile.nameID,
    nameIDFormat: profile.nameIDFormat,
    nameQualifier: profile.nameQualifier,
    spNameQualifier: profile.spNameQualifier,
    sessionIndex: profile.sessionIndex
  }
}
