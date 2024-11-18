// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { z } from 'zod'

import { authenticateProfile } from '../shared/saml/index.js'
import { employeeLogin } from '../shared/service-client.js'

const Profile = z.object({
  id: z.string(),
  email: z.string().optional(),
  firstName: z.string(),
  lastName: z.string()
})

export const authenticateKeycloakEmployee = authenticateProfile(
  Profile,
  async (profile) => {
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
  }
)
