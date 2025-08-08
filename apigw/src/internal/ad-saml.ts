// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { SAML } from '@node-saml/node-saml'
import { z } from 'zod'

import type { EvakaSamlConfig } from '../shared/config.ts'
import type { RedisClient } from '../shared/redis-client.ts'
import { createSamlIntegration } from '../shared/routes/saml.ts'
import { authenticateProfile, createSamlConfig } from '../shared/saml/index.ts'
import redisCacheProvider from '../shared/saml/node-saml-cache-redis.ts'
import { employeeLogin } from '../shared/service-client.ts'
import type { Sessions } from '../shared/session.ts'

const AD_GIVEN_NAME_KEY =
  'http://schemas.xmlsoap.org/ws/2005/05/identity/claims/givenname'
const AD_FAMILY_NAME_KEY =
  'http://schemas.xmlsoap.org/ws/2005/05/identity/claims/surname'
const AD_EMAIL_KEY =
  'http://schemas.xmlsoap.org/ws/2005/05/identity/claims/emailaddress'
const AD_EMPLOYEE_NUMBER_KEY =
  'http://schemas.xmlsoap.org/ws/2005/05/identity/claims/employeenumber'

const Profile = z
  .object({
    [AD_GIVEN_NAME_KEY]: z.string(),
    [AD_FAMILY_NAME_KEY]: z.string(),
    [AD_EMAIL_KEY]: z.string().optional(),
    [AD_EMPLOYEE_NUMBER_KEY]: z.string().toLowerCase().optional()
  })
  .passthrough()

export function createSamlAdIntegration(
  sessions: Sessions<'employee'>,
  config: {
    externalIdPrefix: string
    userIdKey: string
    saml: EvakaSamlConfig
  },
  redisClient: RedisClient
) {
  const authenticate = authenticateProfile(
    Profile,
    async (req, samlSession, profile) => {
      const aad = profile[config.userIdKey]
      if (!aad || typeof aad !== 'string')
        throw Error('No user ID in SAML data')
      const person = await employeeLogin(req, {
        externalId: `${config.externalIdPrefix}:${aad}`,
        firstName: profile[AD_GIVEN_NAME_KEY] ?? '',
        lastName: profile[AD_FAMILY_NAME_KEY] ?? '',
        email: profile[AD_EMAIL_KEY],
        employeeNumber: profile[AD_EMPLOYEE_NUMBER_KEY]
      })
      return {
        id: person.id,
        authType: 'ad',
        userType: 'EMPLOYEE',
        globalRoles: person.globalRoles,
        allScopedRoles: person.allScopedRoles,
        samlSession
      }
    }
  )
  return createSamlIntegration({
    sessions,
    strategyName: 'ead',
    saml: new SAML(
      createSamlConfig(
        config.saml,
        redisCacheProvider(redisClient, { keyPrefix: 'ad-saml-resp:' }),
        false // wantAuthnResponseSigned as Entra ID does not sign responses
      )
    ),
    authenticate,
    defaultPageUrl: '/employee'
  })
}
