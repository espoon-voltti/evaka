// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { SamlConfig, Strategy } from '@node-saml/passport-saml'
import { z } from 'zod'

import { logWarn } from '../shared/logging.js'
import { createSamlStrategy } from '../shared/saml/index.js'
import { citizenLogin } from '../shared/service-client.js'
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

const ssnRegex = /^[0-9]{6}[-+ABCDEFUVWXY][0-9]{3}[0-9ABCDEFHJKLMNPRSTUVWXY]$/

export function createSuomiFiStrategy(
  sessions: Sessions,
  config: SamlConfig
): Strategy {
  return createSamlStrategy(sessions, config, Profile, async (profile) => {
    const socialSecurityNumber = profile[SUOMI_FI_SSN_KEY]?.trim()
    if (!socialSecurityNumber) throw Error('No SSN in SAML data')
    if (!ssnRegex.test(socialSecurityNumber)) {
      logWarn('Invalid SSN received from Suomi.fi login')
    }
    console.log('HERE suomi-fi-saml')
    const person = await citizenLogin(
      {
        socialSecurityNumber,
        firstName: profile[SUOMI_FI_GIVEN_NAME_KEY]?.trim() ?? '',
        lastName: profile[SUOMI_FI_SURNAME_KEY]?.trim() ?? ''
      },
      undefined
    )
    return {
      id: person.id,
      userType: 'CITIZEN_STRONG',
      globalRoles: [],
      allScopedRoles: []
    }
  })
}
