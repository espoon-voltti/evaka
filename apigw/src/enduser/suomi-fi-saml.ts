// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { SAML } from '@node-saml/node-saml'
import express from 'express'
import { z } from 'zod'

import type { EvakaSamlConfig } from '../shared/config.ts'
import { logWarn } from '../shared/logging.ts'
import type { RedisClient } from '../shared/redis-client.ts'
import { createSamlIntegration } from '../shared/routes/saml.ts'
import { authenticateProfile, createSamlConfig } from '../shared/saml/index.ts'
import redisCacheProvider from '../shared/saml/node-saml-cache-redis.ts'
import { citizenLogin, employeeSuomiFiLogin } from '../shared/service-client.ts'
import type { Sessions } from '../shared/session.ts'
import { linkMessagingSession } from '../shared/session.ts'

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
      createdAt: Date.now(),
      samlSession
    }
  }
)

export function createCitizenSuomiFiIntegration(
  sessions: Sessions<'citizen'>,
  samlConfig: EvakaSamlConfig,
  redisClient: RedisClient,
  citizenCookieSecret: string,
  secondaryCookieConfig?: {
    cookieName: string
    cookieSecret: string
  },
  messagingSessions?: Sessions<'citizen-messaging'>
) {
  const baseIntegration = createSamlIntegration({
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
    citizenCookieSecret,
    secondaryCookieConfig
  })

  if (!messagingSessions) {
    return baseIntegration
  }

  // Create custom router that also handles messaging sessions
  const router = express.Router()
  router.use(sessions.middleware)

  // Login endpoint - delegate to base integration
  router.get('/login', (req, res, next) =>
    baseIntegration.router(req, res, next)
  )

  // Custom login callback that creates both sessions
  router.post(
    '/login/callback',
    express.urlencoded({ extended: false }),
    (req, res, next) => {
      // Run messaging session middleware to prepare req.session for messaging
      messagingSessions.middleware(req, res, (err) => {
        if (err) return next(err)

        // Store the primary session ID before creating messaging session
        const primarySessionId = req.session?.id

        // Now run the base integration's login callback
        baseIntegration.router(req, res, (callbackErr) => {
          if (callbackErr) return next(callbackErr)

          // If primary login succeeded, create messaging session too
          const user = sessions.getUser(req)
          if (user && user.authType === 'sfi') {
            messagingSessions
              .login(req, user)
              .then(() => {
                const messagingSessionId = req.session?.id

                // Link the sessions in Redis for cleanup
                if (
                  primarySessionId &&
                  messagingSessionId &&
                  primarySessionId !== messagingSessionId
                ) {
                  return linkMessagingSession(
                    redisClient,
                    primarySessionId,
                    messagingSessionId,
                    43200 // 30 days in minutes
                  )
                }
              })
              .catch((msgErr) => {
                logWarn(`Failed to create messaging session: ${String(msgErr)}`)
                // Continue even if messaging session fails
              })
          }
        })
      })
    }
  )

  // Logout endpoints - destroy both sessions
  router.get('/logout/callback', (req, res, next) =>
    baseIntegration.router(req, res, next)
  )
  router.post('/logout/callback', (req, res, next) =>
    baseIntegration.router(req, res, next)
  )

  return {
    router,
    logout: async (req: express.Request, res: express.Response) => {
      // Let the base integration handle the logout flow
      // The Redis cleanup in logoutWithToken and destroy will handle messaging sessions
      return baseIntegration.logout(req, res)
    }
  }
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
      createdAt: Date.now(),
      samlSession
    }
  }
)

export function createEmployeeSuomiFiIntegration(
  sessions: Sessions<'employee'>,
  config: EvakaSamlConfig,
  redisClient: RedisClient,
  secondaryCookieConfig?: {
    cookieName: string
    cookieSecret: string
  }
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
    defaultPageUrl: '/employee',
    secondaryCookieConfig
  })
}
