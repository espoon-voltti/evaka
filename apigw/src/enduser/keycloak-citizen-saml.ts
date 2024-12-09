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
import { citizenLogin } from '../shared/service-client.js'
import { Sessions } from '../shared/session.js'

const Profile = z.object({
  socialSecurityNumber: z.string(),
  firstName: z.string(),
  lastName: z.string(),
  email: z.string()
})

export const authenticate = authenticateProfile(
  Profile,
  async (samlSession, profile) => {
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
      authType: 'keycloak-citizen',
      userType: 'CITIZEN_WEAK',
      samlSession
    }
  }
)

export function createKeycloakCitizenIntegration(
  sessions: Sessions<'citizen'>,
  config: EvakaSamlConfig,
  redisClient: RedisClient
) {
  return createSamlIntegration({
    sessions,
    strategyName: 'evaka-customer',
    saml: new SAML(
      createSamlConfig(
        config,
        redisCacheProvider(redisClient, { keyPrefix: 'customer-saml-resp:' })
      )
    ),
    authenticate,
    defaultPageUrl: '/'
  })
}
