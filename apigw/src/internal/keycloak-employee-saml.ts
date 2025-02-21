// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { SAML } from '@node-saml/node-saml'
import { z } from 'zod'

import { EvakaSamlConfig } from '../shared/config.js'
import { RedisClient } from '../shared/redis-client.js'
import { createSamlIntegration } from '../shared/routes/saml.js'
import { authenticateProfile, createSamlConfig } from '../shared/saml/index.js'
import redisCacheProvider from '../shared/saml/node-saml-cache-redis.js'
import { employeeLogin } from '../shared/service-client.js'
import { Sessions } from '../shared/session.js'

const Profile = z.object({
  id: z.string(),
  email: z.string().optional(),
  firstName: z.string(),
  lastName: z.string()
})

const authenticate = authenticateProfile(
  Profile,
  async (req, samlSession, profile) => {
    const id = profile.id
    if (!id) throw Error('No user ID in evaka IDP SAML data')
    const person = await employeeLogin(req, {
      externalId: `evaka:${id}`,
      firstName: profile.firstName ?? '',
      lastName: profile.lastName ?? '',
      email: profile.email
    })
    return {
      id: person.id,
      authType: 'keycloak-employee',
      userType: 'EMPLOYEE',
      globalRoles: person.globalRoles,
      allScopedRoles: person.allScopedRoles,
      samlSession
    }
  }
)

export function createKeycloakEmployeeIntegration(
  sessions: Sessions<'employee'>,
  config: EvakaSamlConfig,
  redisClient: RedisClient
) {
  return createSamlIntegration({
    sessions,
    strategyName: 'evaka',
    saml: new SAML(
      createSamlConfig(
        config,
        redisCacheProvider(redisClient, { keyPrefix: 'keycloak-saml-resp:' })
      )
    ),
    authenticate,
    defaultPageUrl: '/employee'
  })
}
