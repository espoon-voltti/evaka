// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { SamlConfig, Strategy as SamlStrategy } from '@node-saml/passport-saml'
import { employeeLogin } from '../shared/service-client.js'
import { createSamlStrategy } from '../shared/saml/index.js'
import { z } from 'zod'
import { Sessions } from '../shared/session.js'

const Profile = z.object({
  id: z.string(),
  email: z.string().optional(),
  firstName: z.string(),
  lastName: z.string()
})

export function createKeycloakEmployeeSamlStrategy(
  sessions: Sessions,
  config: SamlConfig
): SamlStrategy {
  return createSamlStrategy(sessions, config, Profile, async (profile) => {
    const id = profile.id
    if (!id) throw Error('No user ID in evaka IDP SAML data')
    const person = await employeeLogin({
      externalId: `evaka:${id}`,
      firstName: profile.firstName ?? '',
      lastName: profile.lastName ?? '',
      email: profile.email
    })
    return {
      id: person.id,
      userType: 'EMPLOYEE',
      globalRoles: person.globalRoles,
      allScopedRoles: person.allScopedRoles
    }
  })
}
