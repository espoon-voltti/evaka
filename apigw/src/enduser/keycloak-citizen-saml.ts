// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { SamlConfig, Strategy as SamlStrategy } from '@node-saml/passport-saml'
import { z } from 'zod'

import { createSamlStrategy } from '../shared/saml/index.js'
import { citizenLogin } from '../shared/service-client.js'
import { Sessions } from '../shared/session.js'

const Profile = z.object({
  socialSecurityNumber: z.string(),
  firstName: z.string(),
  lastName: z.string(),
  email: z.string()
})

export function createKeycloakCitizenSamlStrategy(
  sessions: Sessions,
  config: SamlConfig
): SamlStrategy {
  return createSamlStrategy(sessions, config, Profile, async (profile) => {
    const socialSecurityNumber = profile.socialSecurityNumber
    if (!socialSecurityNumber)
      throw Error('No socialSecurityNumber in evaka IDP SAML data')

    const person = await citizenLogin({
      socialSecurityNumber,
      firstName: profile.firstName ?? '',
      lastName: profile.lastName ?? '',
      keycloakEmail: profile.email ?? ''
    })

    return {
      id: person.id,
      userType: 'CITIZEN_WEAK',
      globalRoles: ['CITIZEN_WEAK'],
      allScopedRoles: []
    }
  })
}
