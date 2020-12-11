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
import { evakaSamlCallbackUrl, evakaSamlEntrypoint } from '../config'

export default function createKeycloakSamlStrategy(): SamlStrategy {
  return new SamlStrategy(
    {
      issuer: 'evaka',
      callbackUrl: evakaSamlCallbackUrl,
      entryPoint: evakaSamlEntrypoint,
      logoutUrl: evakaSamlEntrypoint,
      acceptedClockSkewMs: -1
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
  nameID: string
  ID?: string
  nameIDFormat?: string
  nameQualifier?: string
  spNameQualifier?: string
  sessionIndex?: string
}

async function verifyKeycloakProfile(
  profile: KeycloakProfile
): Promise<SamlUser> {
  if (!profile.ID) throw Error('No user ID in evaka IDP SAML data')
  const person = await getOrCreateEmployee({
    externalId: `evaka:${profile.ID}`,
    firstName: profile.nameID.split('.')[0],
    lastName: profile.nameID.split('.')[1].split('@')[0],
    email: profile.nameID
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
