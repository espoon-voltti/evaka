// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { SamlConfig, Strategy as SamlStrategy } from '@node-saml/passport-saml'
import { z } from 'zod'

import { Config } from '../shared/config.js'
import { createSamlStrategy } from '../shared/saml/index.js'
import { employeeLogin } from '../shared/service-client.js'
import { Sessions } from '../shared/session.js'

const AD_GIVEN_NAME_KEY =
  'http://schemas.xmlsoap.org/ws/2005/05/identity/claims/givenname'
const AD_FAMILY_NAME_KEY =
  'http://schemas.xmlsoap.org/ws/2005/05/identity/claims/surname'
const AD_EMAIL_KEY =
  'http://schemas.xmlsoap.org/ws/2005/05/identity/claims/emailaddress'
const AD_EMPLOYEE_NUMBER_KEY =
  'http://schemas.xmlsoap.org/ws/2005/05/identity/claims/employeenumber'

export function createAdSamlStrategy(
  sessions: Sessions,
  config: Config['ad'],
  samlConfig: SamlConfig
): SamlStrategy {
  const Profile = z
    .object({
      [AD_GIVEN_NAME_KEY]: z.string(),
      [AD_FAMILY_NAME_KEY]: z.string(),
      [AD_EMAIL_KEY]: z.string().optional(),
      [AD_EMPLOYEE_NUMBER_KEY]: z.string().toLowerCase().optional()
    })
    .passthrough()
  return createSamlStrategy(sessions, samlConfig, Profile, async (profile) => {
    const aad = profile[config.userIdKey]
    if (!aad || typeof aad !== 'string') throw Error('No user ID in SAML data')
    const person = await employeeLogin({
      externalId: `${config.externalIdPrefix}:${aad}`,
      firstName: profile[AD_GIVEN_NAME_KEY] ?? '',
      lastName: profile[AD_FAMILY_NAME_KEY] ?? '',
      email: profile[AD_EMAIL_KEY],
      employeeNumber: profile[AD_EMPLOYEE_NUMBER_KEY]
    }, undefined)
    return {
      id: person.id,
      userType: 'EMPLOYEE',
      globalRoles: person.globalRoles,
      allScopedRoles: person.allScopedRoles
    }
  })
}
