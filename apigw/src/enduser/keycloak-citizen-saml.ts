// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { z } from 'zod'

import { authenticateProfile } from '../shared/saml/index.js'
import { citizenLogin } from '../shared/service-client.js'

const Profile = z.object({
  socialSecurityNumber: z.string(),
  firstName: z.string(),
  lastName: z.string(),
  email: z.string()
})

export const authenticateKeycloakCitizen = authenticateProfile(
  Profile,
  async (profile) => {
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
      globalRoles: [],
      allScopedRoles: []
    }
  }
)
