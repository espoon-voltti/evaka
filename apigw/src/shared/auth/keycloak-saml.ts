// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import {
  Profile,
  Strategy as SamlStrategy,
  VerifiedCallback
} from 'passport-saml'
import { SamlUser } from '../routes/auth/saml/types'
import { getOrCreateEmployee } from '../service-client'
import { evakaSamlConfig } from '../config'
import certificates from '../certificates'

export default function createKeycloakSamlStrategy(): SamlStrategy {
  return new SamlStrategy(
    {
      issuer: 'evaka',
      callbackUrl: evakaSamlConfig.callbackUrl,
      entryPoint: evakaSamlConfig.entryPoint,
      logoutUrl: evakaSamlConfig.entryPoint,
      acceptedClockSkewMs: -1,
      cert: evakaSamlConfig.publicCert.map(
        (certificateName) => certificates[certificateName]
      ),
      identifierFormat: 'urn:oasis:names:tc:SAML:2.0:nameid-format:transient',
      signatureAlgorithm: 'sha256'
    },
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
    roles: person.roles,
    userType: 'EMPLOYEE',
    nameID: profile.nameID,
    nameIDFormat: profile.nameIDFormat,
    nameQualifier: profile.nameQualifier,
    spNameQualifier: profile.spNameQualifier,
    sessionIndex: profile.sessionIndex
  }
}
