// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { z } from 'zod'
import passportSaml, {
  SamlConfig,
  Strategy as SamlStrategy
} from '@node-saml/passport-saml'
import { citizenLogin } from '../shared/service-client'
import { createSamlStrategy } from '../shared/saml'
import { EvakaSessionUser } from '../shared/auth'

export function createKeycloakCitizenSamlStrategy(
  config: SamlConfig
): SamlStrategy {
  return createSamlStrategy(config, Profile, verifyKeycloakProfile)
}

const Profile = z.object({
  socialSecurityNumber: z.string(),
  firstName: z.string(),
  lastName: z.string()
})

async function verifyKeycloakProfile(
  profile: passportSaml.Profile
): Promise<EvakaSessionUser> {
  const asString = (value: unknown) =>
    value == null ? undefined : String(value)

  const socialSecurityNumber = asString(profile['socialSecurityNumber'])
  if (!socialSecurityNumber)
    throw Error('No socialSecurityNumber in evaka IDP SAML data')

  const person = await citizenLogin({
    socialSecurityNumber,
    firstName: asString(profile['firstName']) ?? '',
    lastName: asString(profile['lastName']) ?? ''
  })

  return {
    id: person.id,
    userType: 'CITIZEN_WEAK',
    globalRoles: ['CITIZEN_WEAK'],
    allScopedRoles: []
  }
}
