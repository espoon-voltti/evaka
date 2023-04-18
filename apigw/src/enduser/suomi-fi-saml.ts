// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { z } from 'zod'
import passportSaml, { SamlConfig, Strategy } from 'passport-saml'
import { citizenLogin } from '../shared/service-client'
import { getCitizenBySsn } from '../shared/dev-api'
import { toSamlVerifyFunction } from '../shared/saml'
import { EvakaSessionUser } from '../shared/auth'
import { DevSfiStrategy } from './dev-sfi-auth'

// Suomi.fi e-Identification – Attributes transmitted on an identified user:
//   https://esuomi.fi/suomi-fi-services/suomi-fi-e-identification/14247-2/?lang=en
// Note: Suomi.fi only returns the values we request in our SAML metadata
const SUOMI_FI_SSN_KEY = 'urn:oid:1.2.246.21'
const SUOMI_FI_GIVEN_NAME_KEY = 'urn:oid:2.5.4.42'
const SUOMI_FI_SURNAME_KEY = 'urn:oid:2.5.4.4'

async function verifyProfile(
  profile: passportSaml.Profile
): Promise<EvakaSessionUser> {
  const asString = (value: unknown) =>
    value == null ? undefined : String(value)

  const socialSecurityNumber = asString(profile[SUOMI_FI_SSN_KEY])
  if (!socialSecurityNumber) throw Error('No SSN in SAML data')
  const person = await citizenLogin({
    socialSecurityNumber,
    firstName: asString(profile[SUOMI_FI_GIVEN_NAME_KEY]) ?? '',
    lastName: asString(profile[SUOMI_FI_SURNAME_KEY]) ?? ''
  })
  return {
    id: person.id,
    userType: 'ENDUSER',
    globalRoles: ['END_USER'],
    allScopedRoles: []
  }
}

const Profile = z.object({
  [SUOMI_FI_SSN_KEY]: z.string(),
  [SUOMI_FI_GIVEN_NAME_KEY]: z.string(),
  [SUOMI_FI_SURNAME_KEY]: z.string()
})

export function createDevSuomiFiStrategy(): DevSfiStrategy {
  return new DevSfiStrategy(async (ssn: string) => {
    const citizen = await getCitizenBySsn(ssn)
    return verifyProfile({
      nameID: 'dummyid',
      [SUOMI_FI_SSN_KEY]: citizen.ssn,
      [SUOMI_FI_GIVEN_NAME_KEY]: citizen.firstName,
      [SUOMI_FI_SURNAME_KEY]: citizen.lastName
    })
  })
}

export default function createSuomiFiStrategy(
  samlConfig: SamlConfig
): Strategy {
  return new Strategy(samlConfig, toSamlVerifyFunction(Profile, verifyProfile))
}
