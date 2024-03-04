// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { z } from 'zod'
import { SamlConfig, Strategy } from '@node-saml/passport-saml'
import { citizenLogin } from '../shared/service-client.js'
import { createSamlStrategy } from '../shared/saml/index.js'
import { Sessions } from '../shared/session.js'

// Suomi.fi e-Identification – Attributes transmitted on an identified user:
//   https://esuomi.fi/suomi-fi-services/suomi-fi-e-identification/14247-2/?lang=en
// Note: Suomi.fi only returns the values we request in our SAML metadata
const SUOMI_FI_SSN_KEY = 'urn:oid:1.2.246.21'
const SUOMI_FI_GIVEN_NAME_KEY = 'urn:oid:2.5.4.42'
const SUOMI_FI_SURNAME_KEY = 'urn:oid:2.5.4.4'

const Profile = z.object({
  [SUOMI_FI_SSN_KEY]: z.string(),
  [SUOMI_FI_GIVEN_NAME_KEY]: z.string(),
  [SUOMI_FI_SURNAME_KEY]: z.string()
})

export function createSuomiFiStrategy(
  sessions: Sessions,
  config: SamlConfig
): Strategy {
  return createSamlStrategy(sessions, config, Profile, async (profile) => {
    const socialSecurityNumber = profile[SUOMI_FI_SSN_KEY]
    if (!socialSecurityNumber) throw Error('No SSN in SAML data')
    const person = await citizenLogin({
      socialSecurityNumber,
      firstName: profile[SUOMI_FI_GIVEN_NAME_KEY] ?? '',
      lastName: profile[SUOMI_FI_SURNAME_KEY] ?? ''
    })
    return {
      id: person.id,
      userType: 'CITIZEN_STRONG',
      globalRoles: [],
      allScopedRoles: []
    }
  })
}
