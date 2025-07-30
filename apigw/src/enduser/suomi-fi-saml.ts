// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { SAML } from '@node-saml/node-saml'
import { z } from 'zod'

import type { EvakaSamlConfig } from '../shared/config.ts'
import { logWarn } from '../shared/logging.ts'
import type { RedisClient } from '../shared/redis-client.ts'
import { createSamlIntegration } from '../shared/routes/saml.ts'
import { authenticateProfile, createSamlConfig } from '../shared/saml/index.ts'
import redisCacheProvider from '../shared/saml/node-saml-cache-redis.ts'
import { citizenLogin, employeeSuomiFiLogin } from '../shared/service-client.ts'
import type { Sessions } from '../shared/session.ts'

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

const authenticateCitizen = authenticateProfile(
  Profile,
  async (req, samlSession, profile) => {
    const socialSecurityNumber = profile[SUOMI_FI_SSN_KEY]?.trim()
    if (!socialSecurityNumber) throw Error('No SSN in SAML data')
    if (!ssnRegex.test(socialSecurityNumber)) {
      logWarn('Invalid SSN received from Suomi.fi login')
    }
    const person = await citizenLogin(req, {
      socialSecurityNumber,
      firstName: profile[SUOMI_FI_GIVEN_NAME_KEY]?.trim() ?? '',
      lastName: profile[SUOMI_FI_SURNAME_KEY]?.trim() ?? ''
    })
    return {
      id: person.id,
      authType: 'sfi',
      userType: 'CITIZEN_STRONG',
      samlSession
    }
  }
)

export function createCitizenSuomiFiIntegration(
  sessions: Sessions<'citizen'>,
  samlConfig: EvakaSamlConfig,
  redisClient: RedisClient,
  citizenCookieSecret: string
) {
  return createSamlIntegration({
    sessions,
    strategyName: 'suomifi',
    saml: new SAML(
      createSamlConfig(
        samlConfig,
        redisCacheProvider(redisClient, { keyPrefix: 'suomifi-saml-resp:' })
      )
    ),
    authenticate: authenticateCitizen,
    defaultPageUrl: '/',
    citizenCookieSecret
  })
}

const authenticateEmployee = authenticateProfile(
  Profile,
  async (req, samlSession, profile) => {
    const socialSecurityNumber = profile[SUOMI_FI_SSN_KEY]?.trim()
    if (!socialSecurityNumber) throw Error('No SSN in SAML data')
    if (!ssnRegex.test(socialSecurityNumber)) {
      logWarn('Invalid SSN received from Suomi.fi login')
    }
    const person = await employeeSuomiFiLogin(req, {
      ssn: profile[SUOMI_FI_SSN_KEY],
      firstName: profile[SUOMI_FI_GIVEN_NAME_KEY],
      lastName: profile[SUOMI_FI_SURNAME_KEY]
    })
    return {
      id: person.id,
      authType: 'sfi',
      userType: 'EMPLOYEE',
      globalRoles: person.globalRoles,
      allScopedRoles: person.allScopedRoles,
      samlSession
    }
  }
)

export function createEmployeeSuomiFiIntegration(
  sessions: Sessions<'employee'>,
  config: EvakaSamlConfig,
  redisClient: RedisClient
) {
  return createSamlIntegration({
    sessions,
    strategyName: 'suomifi',
    saml: new SAML(
      createSamlConfig(
        config,
        redisCacheProvider(redisClient, { keyPrefix: 'employee-sfi:' })
      )
    ),
    authenticate: authenticateEmployee,
    defaultPageUrl: '/employee'
  })
}
