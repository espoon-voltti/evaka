// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { z } from 'zod'
import passportSaml, {
  SamlConfig,
  Strategy as SamlStrategy
} from 'passport-saml'
import { getEmployeeByExternalId, upsertEmployee } from '../shared/dev-api'
import { employeeLogin, UserRole } from '../shared/service-client'
import { Config } from '../shared/config'
import { toSamlVerifyFunction } from '../shared/saml'
import { EvakaSessionUser } from '../shared/auth'
import { DevAdStrategy } from './dev-ad-auth'

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

export function createDevAdStrategy(config: Config['ad']) {
  const getter = async (userId: string) => {
    const employee = await getEmployeeByExternalId(
      `${config.externalIdPrefix}:${userId}`
    )
    return verifyProfile(config, {
      nameID: 'dummyid',
      [config.userIdKey]: userId,
      [AD_GIVEN_NAME_KEY]: employee.firstName,
      [AD_FAMILY_NAME_KEY]: employee.lastName,
      [AD_EMAIL_KEY]: employee.email ? employee.email : ''
    })
  }

  const upserter = async (
    userId: string,
    roles: string[],
    firstName: string,
    lastName: string,
    email: string
  ) => {
    if (!userId) throw Error('No user ID in SAML data')
    await upsertEmployee({
      firstName,
      lastName,
      email,
      externalId: `${config.externalIdPrefix}:${userId}`,
      roles: roles as UserRole[]
    })
    return verifyProfile(config, {
      nameID: 'dummyid',
      [config.userIdKey]: userId,
      [AD_GIVEN_NAME_KEY]: firstName,
      [AD_FAMILY_NAME_KEY]: lastName,
      [AD_EMAIL_KEY]: email
    })
  }

  return new DevAdStrategy(getter, upserter)
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
