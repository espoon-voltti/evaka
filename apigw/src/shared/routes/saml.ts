// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import * as url from 'node:url'

import { AuthOptions, Profile, SAML } from '@node-saml/node-saml'
import { AxiosError } from 'axios'
import express from 'express'
import _ from 'lodash'

import { createLogoutToken, login, logout } from '../auth/index.js'
import { toRequestHandler } from '../express.js'
import { logAuditEvent, logDebug } from '../logging.js'
import {
  parseDescriptionFromSamlError,
  samlErrorSchema
} from '../saml/error-utils.js'
import {
  AuthenticateProfile,
  getRawUnvalidatedRelayState,
  SamlProfileIdSchema,
  SamlProfileSchema,
  validateRelayStateUrl
} from '../saml/index.js'
import { Sessions } from '../session.js'

const urlencodedParser = express.urlencoded({ extended: false })

export interface SamlEndpointConfig {
  sessions: Sessions
  saml: SAML
  strategyName: string
  defaultPageUrl: string
  authenticate: AuthenticateProfile
}

export class SamlError extends Error {
  constructor(
    message: string,
    public options?: {
      /**
       * Redirect the browser to this URL if possible using status 301
       */
      redirectUrl?: string
    }
  ) {
    super(message)
    this.name = 'SamlError'
  }
}

// Configures passport to use the given strategy, and returns an Express router
// for handling SAML-related requests.
//
// We support two SAML "bindings", which define how data is passed by the
// browser to the SP (us) and the IDP.
// * HTTP redirect: the browser makes a GET request with query parameters
// * HTTP POST: the browser makes a POST request with URI-encoded form body
export default function createSamlRouter(
  endpointConfig: SamlEndpointConfig
): express.Router {
  const { sessions, strategyName, saml, defaultPageUrl, authenticate } =
    endpointConfig

  const eventCode = (name: string) => `evaka.saml.${strategyName}.${name}`
  const errorRedirectUrl = (err: unknown) => {
    let errorCode: string | undefined = undefined
    if (err instanceof AxiosError) {
      // eslint-disable-next-line @typescript-eslint/no-unsafe-member-access,@typescript-eslint/no-unsafe-call,@typescript-eslint/no-unsafe-assignment
      errorCode = err.response?.data?.errorCode?.toString() ?? undefined
    }
    return errorCode
      ? `${defaultPageUrl}?loginError=true&errorCode=${encodeURIComponent(errorCode)}`
      : `${defaultPageUrl}?loginError=true`
  }
  const samlRequestOptions = (req: express.Request): AuthOptions => {
    const locale = req.query.locale
    return typeof locale === 'string' ? { additionalParams: { locale } } : {}
  }

  const isSamlPostRequest = (req: express.Request) => 'SAMLRequest' in req.body

  const validateSamlLoginResponse = async (
    req: express.Request
  ): Promise<Profile> => {
    // eslint-disable-next-line @typescript-eslint/no-unsafe-argument
    const samlMessage = await saml.validatePostResponseAsync(req.body)
    if (samlMessage.loggedOut || !samlMessage.profile) {
      throw new Error('Invalid SAML message type: expected login response')
    }
    return samlMessage.profile
  }

  const router = express.Router()

  // Our application directs the browser to this endpoint to start the login
  // flow. We generate a LoginRequest.
  router.get(
    `/login`,
    toRequestHandler(async (req, res) => {
      logAuditEvent(eventCode('sign_in_started'), req, 'Login endpoint called')
      try {
        const idpLoginUrl = await saml.getAuthorizeUrlAsync(
          // no need for validation here, because the value only matters in the login callback request and is validated there
          getRawUnvalidatedRelayState(req) ?? '',
          undefined,
          samlRequestOptions(req)
        )
        return res.redirect(idpLoginUrl)
      } catch (err) {
        logAuditEvent(
          eventCode('sign_in_failed'),
          req,
          `Error logging user in. Error: ${err?.toString()}`
        )
        throw new SamlError('Login failed', {
          redirectUrl: errorRedirectUrl(err)
        })
      }
    })
  )
  // The IDP makes the browser POST to this callback during login flow, and
  // a SAML LoginResponse is included in the request.
  router.post(
    `/login/callback`,
    urlencodedParser,
    toRequestHandler(async (req, res) => {
      logAuditEvent(eventCode('sign_in'), req, 'Login callback endpoint called')
      let profile: Profile
      try {
        profile = await validateSamlLoginResponse(req)
      } catch (err) {
        if (
          err instanceof Error &&
          err.message === 'InResponseTo is not valid'
        ) {
          // When user uses browse back functionality after login we get invalid InResponseTo
          // This will ignore the error
          const redirectUrl = validateRelayStateUrl(req) ?? defaultPageUrl
          logDebug(`Redirecting to ${redirectUrl}`, req, { redirectUrl })
          return res.redirect(redirectUrl)
        }

        const samlError = samlErrorSchema.safeParse(err)
        const description =
          (samlError.success
            ? parseDescriptionFromSamlError(samlError.data, req)
            : undefined) || 'Could not parse SAML message'
        logAuditEvent(
          eventCode('sign_in_failed'),
          req,
          `Failed to authenticate user. Description: ${description}. Error: ${err?.toString()}`
        )
        throw new SamlError('Login failed', {
          redirectUrl: errorRedirectUrl(err)
        })
      }
      try {
        const user = await authenticate(profile)
        await login(req, {
          ...user,
          ...SamlProfileSchema.parse(profile)
        })
        logAuditEvent(
          `evaka.saml.${strategyName}.sign_in`,
          req,
          'User logged in successfully'
        )

        // Persist in session to allow custom logic per strategy
        req.session.idpProvider = strategyName
        await sessions.saveLogoutToken(req, createLogoutToken(profile))

        const redirectUrl = validateRelayStateUrl(req) ?? defaultPageUrl
        logDebug(`Redirecting to ${redirectUrl}`, req, { redirectUrl })
        return res.redirect(redirectUrl)
      } catch (err) {
        logAuditEvent(
          eventCode('sign_in_failed'),
          req,
          `Error logging user in. Error: ${err?.toString()}`
        )
        throw new SamlError('Login failed', {
          redirectUrl: errorRedirectUrl(err)
        })
      }
    })
  )

  // Our application directs the browser to one of these endpoints to start
  // the logout flow. We generate a LogoutRequest.
  router.get(
    `/logout`,
    toRequestHandler(async (req, res) => {
      logAuditEvent(
        eventCode('sign_out_requested'),
        req,
        'Logout endpoint called'
      )
      try {
        const profile = SamlProfileSchema.safeParse(req.user)
        let url: string
        if (profile.success) {
          url = await saml.getLogoutUrlAsync(
            profile.data,
            // no need for validation here, because the value only matters in the logout callback request and is validated there
            getRawUnvalidatedRelayState(req) ?? '',
            samlRequestOptions(req)
          )
        } else {
          url = defaultPageUrl
        }
        logDebug('Logging user out from passport.', req)
        await logout(sessions, req, res)
        return res.redirect(url)
      } catch (err) {
        logAuditEvent(
          eventCode('sign_out_failed'),
          req,
          `Logout failed. Error: ${err?.toString()}.`
        )
        throw new SamlError('Logout failed', { redirectUrl: defaultPageUrl })
      }
    })
  )
  const logoutCallback = (
    parseLogoutMessage: (req: express.Request) => Promise<Profile | null>
  ) =>
    toRequestHandler(async (req, res) => {
      logAuditEvent(eventCode('sign_out'), req, 'Logout callback called')
      try {
        const profile = await parseLogoutMessage(req)
        let url: string
        // There are two scenarios:
        // 1. IDP-initiated logout, and we've just received a logout request -> profile is not null, the SAML transaction
        // is still in progress, and we should redirect the user back to the IDP
        // 2. SP-initiated logout, and we've just received a logout response -> profile is null, the SAML transaction
        // is complete, and we should redirect the user to some meaningful page
        if (profile) {
          let user: unknown
          if (req.user) {
            const userId = SamlProfileIdSchema.safeParse(req.user)
            user = userId.success ? userId.data : undefined

            await logout(sessions, req, res)
          } else {
            // We're possibly doing SLO without a real session (e.g. browser has
            // 3rd party cookies disabled)
            const logoutToken = createLogoutToken(profile)
            const sessionUser = await sessions.logoutWithToken(logoutToken)
            const userId = SamlProfileIdSchema.safeParse(sessionUser)
            user = userId.success ? userId.data : undefined
          }
          const profileId = SamlProfileIdSchema.safeParse(profile)
          const success = profileId.success && _.isEqual(user, profileId.data)

          url = await saml.getLogoutResponseUrlAsync(
            profile,
            // not validated, because the value and its format are specified by the IDP and we're supposed to
            // just pass it back
            getRawUnvalidatedRelayState(req) ?? '',
            samlRequestOptions(req),
            success
          )
        } else {
          url = validateRelayStateUrl(req) ?? defaultPageUrl
        }
        return res.redirect(url)
      } catch (err) {
        logAuditEvent(
          eventCode('sign_out_failed'),
          req,
          `Logout failed. Error: ${err?.toString()}.`
        )
        throw new SamlError('Logout failed', { redirectUrl: defaultPageUrl })
      }
    })
  // The IDP makes the browser either GET or POST one of these endpoints in two
  // separate logout flows.
  // 1. SP-initiated logout. In this case the logout flow started from us
  //   (= /auth/saml/logout endpoint), and a SAML LogoutResponse is included
  //   in the request.
  // 2. IDP-initiated logout (= SAML single logout). In this case the logout
  //   flow started from the IDP, and a SAML LogoutRequest is included in the
  //   request.
  router.get(
    `/logout/callback`,
    logoutCallback(async (req) => {
      const originalQuery = url.parse(req.url).query ?? ''
      const { profile, loggedOut } = await saml.validateRedirectAsync(
        req.query,
        originalQuery
      )
      if (!loggedOut) {
        throw new SamlError(
          'Invalid SAML message type: expected logout response'
        )
      }
      return profile
    })
  )
  router.post(
    `/logout/callback`,
    urlencodedParser,
    logoutCallback(async (req) => {
      const { profile, loggedOut } = isSamlPostRequest(req)
        ? // eslint-disable-next-line @typescript-eslint/no-unsafe-argument
          await saml.validatePostRequestAsync(req.body)
        : // eslint-disable-next-line @typescript-eslint/no-unsafe-argument
          await saml.validatePostResponseAsync(req.body)
      if (!loggedOut) {
        throw new SamlError(
          'Invalid SAML message type: expected logout request/response'
        )
      }
      return profile
    })
  )

  return router
}
