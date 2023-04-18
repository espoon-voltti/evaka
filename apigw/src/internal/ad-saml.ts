// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { z } from 'zod'
import passportSaml, {
  SamlConfig,
  Strategy as SamlStrategy
} from 'passport-saml'
import { employeeLogin } from '../shared/service-client'
import { Config } from '../shared/config'
import { toSamlVerifyFunction } from '../shared/saml'
import { EvakaSessionUser } from '../shared/auth'

const AD_GIVEN_NAME_KEY =
  'http://schemas.xmlsoap.org/ws/2005/05/identity/claims/givenname'
const AD_FAMILY_NAME_KEY =
  'http://schemas.xmlsoap.org/ws/2005/05/identity/claims/surname'
const AD_EMAIL_KEY =
  'http://schemas.xmlsoap.org/ws/2005/05/identity/claims/emailaddress'
const AD_EMPLOYEE_NUMBER_KEY =
  'http://schemas.xmlsoap.org/ws/2005/05/identity/claims/employeenumber'

async function verifyProfile(
  config: Config['ad'],
  profile: passportSaml.Profile
): Promise<EvakaSessionUser> {
  const asString = (value: unknown) =>
    value == null ? undefined : String(value)

  const aad = profile[config.userIdKey]
  if (!config.mock && !aad) throw Error('No user ID in SAML data')
  const person = await employeeLogin({
    externalId: `${config.externalIdPrefix}:${aad}`,
    firstName: asString(profile[AD_GIVEN_NAME_KEY]) ?? '',
    lastName: asString(profile[AD_FAMILY_NAME_KEY]) ?? '',
    email: asString(profile[AD_EMAIL_KEY]),
    employeeNumber: asString(profile[AD_EMPLOYEE_NUMBER_KEY])
  })
  return {
    id: person.id,
    userType: 'EMPLOYEE',
    globalRoles: person.globalRoles,
    allScopedRoles: person.allScopedRoles
  }
}

export default function createAdStrategy(
  config: Config['ad'],
  samlConfig: SamlConfig
): SamlStrategy {
  const Profile = z.object({
    [config.userIdKey]: z.string(),
    [AD_GIVEN_NAME_KEY]: z.string(),
    [AD_FAMILY_NAME_KEY]: z.string(),
    [AD_EMAIL_KEY]: z.string().optional(),
    [AD_EMPLOYEE_NUMBER_KEY]: z.string().optional()
  })
  return new SamlStrategy(
    samlConfig,
    toSamlVerifyFunction(Profile, (profile) => verifyProfile(config, profile))
  )
}
