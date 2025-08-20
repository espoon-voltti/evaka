// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import * as url from 'node:url'

import type { AuthOptions, Profile, SAML } from '@node-saml/node-saml'
import { AxiosError } from 'axios'
import cookieParser from 'cookie-parser'
import express from 'express'
import _ from 'lodash'

import { createLogoutToken } from '../auth/index.ts'
import { setDeviceAuthHistoryCookie } from '../device-cookies.ts'
import type { AsyncRequestHandler } from '../express.ts'
import { toRequestHandler } from '../express.ts'
import { logAuditEvent, logDebug } from '../logging.ts'
import {
  parseDescriptionFromSamlError,
  samlErrorSchema
} from '../saml/error-utils.ts'
import type { AuthenticateProfile } from '../saml/index.ts'
import {
  getRawUnvalidatedRelayState,
  SamlProfileIdSchema,
  SamlSessionSchema,
  validateRelayStateUrl
} from '../saml/index.ts'
import type { Sessions, SessionType } from '../session.ts'

const urlencodedParser = express.urlencoded({ extended: false })

export interface SamlEndpointConfig<T extends SessionType> {
  sessions: Sessions<T>
  saml: SAML
  strategyName: string
  defaultPageUrl: string
  authenticate: AuthenticateProfile
  citizenCookieSecret?: string
}

export type Options = {
  /**
   * True if this error should not be logged by the error middleware
   */
  silent?: boolean
  /**
   * Redirect the browser to this URL if possible using status 301
   */
  redirectUrl?: string
} & ErrorOptions

export class SamlError extends Error {
  public options: Options | undefined

  constructor(message: string, options?: Options) {
    super(message, options)
    this.name = 'SamlError'
    this.options = options
  }
}

const samlRequestOptions = (req: express.Request): AuthOptions => {
  const locale = req.query.locale
  return typeof locale === 'string' ? { additionalParams: { locale } } : {}
}

const isSamlPostRequest = (req: express.Request) => 'SAMLRequest' in req.body

type SamlAuditEvent =
  | 'sign_in_started'
  | 'sign_in'
  | 'sign_in_failed'
  | 'sign_out_requested'
  | 'sign_out'
  | 'sign_out_failed'

export interface SamlIntegration {
  router: express.Router
  logout: AsyncRequestHandler
}

export function createSamlIntegration<T extends SessionType>(
  endpointConfig: SamlEndpointConfig<T>
): SamlIntegration {
  const {
    sessions,
    strategyName,
    saml,
    defaultPageUrl,
    authenticate,
    citizenCookieSecret
  } = endpointConfig

  const eventCode = (name: SamlAuditEvent) =>
    `evaka.saml.${strategyName}.${name}`
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

  const validateSamlLogoutMessage = async (
    req: express.Request
  ): Promise<Profile | null> => {
    let samlMessage: { profile: Profile | null; loggedOut: boolean }
    if (req.method === 'GET') {
      const originalQuery = url.parse(req.url).query ?? ''
      samlMessage = await saml.validateRedirectAsync(req.query, originalQuery)
    } else if (req.method === 'POST') {
      samlMessage = isSamlPostRequest(req)
        ? // eslint-disable-next-line @typescript-eslint/no-unsafe-argument
          await saml.validatePostRequestAsync(req.body)
        : // eslint-disable-next-line @typescript-eslint/no-unsafe-argument
          await saml.validatePostResponseAsync(req.body)
    } else {
      throw new SamlError(`Unsupported HTTP method ${req.method}`)
    }
    if (!samlMessage.loggedOut) {
      throw new SamlError(
        'Invalid SAML message type: expected logout request/response'
      )
    }
    return samlMessage.profile
  }

  const login: AsyncRequestHandler = async (req, res) => {
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
        `Error logging user in. ${err?.toString()}`
      )
      throw new SamlError('Login failed', {
        redirectUrl: errorRedirectUrl(err),
        cause: err
      })
    }
  }
  const loginCallback: AsyncRequestHandler = async (req, res) => {
    logAuditEvent(eventCode('sign_in'), req, 'Login callback endpoint called')
    let profile: Profile
    try {
      profile = await validateSamlLoginResponse(req)
    } catch (err) {
      if (err instanceof Error && err.message === 'InResponseTo is not valid')
        // These errors can happen for example when the user browses back to the login callback after login
        throw new SamlError('Login failed', {
          redirectUrl: sessions.isAuthenticated(req)
            ? (validateRelayStateUrl(req)?.toString() ?? defaultPageUrl)
            : errorRedirectUrl(err),
          cause: err,
          // just ignore without logging to reduce noise in logs
          silent: true
        })

      const samlError = samlErrorSchema.safeParse(err)
      if (samlError.success) {
        const description =
          parseDescriptionFromSamlError(samlError.data, req) ??
          'Could not parse SAML message'
        logAuditEvent(
          eventCode('sign_in_failed'),
          req,
          `Failed to authenticate user. Description: ${description}. ${err?.toString()}`
        )
        throw new SamlError('Login failed', {
          redirectUrl: errorRedirectUrl(err),
          cause: err,
          // just ignore without logging to reduce noise in logs
          silent: true
        })
      } else {
        logAuditEvent(
          eventCode('sign_in_failed'),
          req,
          `Failed to authenticate user. ${err?.toString()}`
        )
        throw new SamlError('Login failed', {
          redirectUrl: errorRedirectUrl(err),
          cause: err
        })
      }
    }
    try {
      const user = await authenticate(req, profile)
      await sessions.login(req, user)
      logAuditEvent(eventCode('sign_in'), req, 'User logged in successfully')

      // Set device cookie for citizen authentication
      if (citizenCookieSecret) {
        setDeviceAuthHistoryCookie(res, user.id)
      }

      // Persist in session to allow custom logic per strategy
      req.session.idpProvider = strategyName
      await sessions.saveLogoutToken(req, createLogoutToken(profile))

      const redirectUrl =
        validateRelayStateUrl(req)?.toString() ?? defaultPageUrl
      logDebug(`Redirecting to ${redirectUrl}`, req, { redirectUrl })
      return res.redirect(redirectUrl)
    } catch (err) {
      logAuditEvent(
        eventCode('sign_in_failed'),
        req,
        `Error logging user in. ${err?.toString()}`
      )
      throw new SamlError('Login failed', {
        redirectUrl: errorRedirectUrl(err),
        cause: err
      })
    }
  }

  const logout: AsyncRequestHandler = async (req, res) => {
    logAuditEvent(
      eventCode('sign_out_requested'),
      req,
      'Logout endpoint called'
    )
    try {
      const user = sessions.getUser(req)
      const samlSession = SamlSessionSchema.safeParse(user)
      let url: string
      if (samlSession.success) {
        url = await saml.getLogoutUrlAsync(
          samlSession.data,
          // no need for validation here, because the value only matters in the logout callback request and is validated there
          getRawUnvalidatedRelayState(req) ?? '',
          samlRequestOptions(req)
        )
      } else {
        url = defaultPageUrl
      }
      await sessions.destroy(req, res)
      return res.redirect(url)
    } catch (err) {
      logAuditEvent(
        eventCode('sign_out_failed'),
        req,
        `Logout failed. ${err?.toString()}.`
      )
      throw new SamlError('Logout failed', {
        redirectUrl: defaultPageUrl,
        cause: err
      })
    }
  }
  const logoutCallback: AsyncRequestHandler = async (req, res) => {
    logAuditEvent(eventCode('sign_out'), req, 'Logout callback called')

    try {
      const profile = await validateSamlLogoutMessage(req)
      let url: string
      // There are two scenarios:
      // 1. IDP-initiated logout, and we've just received a logout request -> profile is not null, the SAML transaction
      // is still in progress, and we should redirect the user back to the IDP
      // 2. SP-initiated logout, and we've just received a logout response -> profile is null, the SAML transaction
      // is complete, and we should redirect the user to some meaningful page
      if (profile) {
        let user: unknown
        const sessionUser = sessions.getUser(req)
        if (sessionUser) {
          const userId = SamlProfileIdSchema.safeParse(sessionUser)
          user = userId.success ? userId.data : undefined

          await sessions.destroy(req, res)
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
        url = validateRelayStateUrl(req)?.toString() ?? defaultPageUrl
      }
      return res.redirect(url)
    } catch (err) {
      if (err instanceof Error && err.message === 'InResponseTo is not valid')
        throw new SamlError('Logout failed', {
          redirectUrl: validateRelayStateUrl(req)?.toString() ?? defaultPageUrl,
          cause: err,
          // just ignore without logging to reduce noise in logs
          silent: true
        })

      const samlError = samlErrorSchema.safeParse(err)
      if (samlError.success) {
        const description =
          parseDescriptionFromSamlError(samlError.data, req) ??
          'Could not parse SAML message'
        logAuditEvent(
          eventCode('sign_out_failed'),
          req,
          `Logout failed. Description: ${description}. ${err?.toString()}`
        )
        throw new SamlError('Logout failed', {
          redirectUrl: validateRelayStateUrl(req)?.toString() ?? defaultPageUrl,
          cause: err,
          // just ignore without logging to reduce noise in logs
          silent: true
        })
      } else {
        logAuditEvent(
          eventCode('sign_out_failed'),
          req,
          `Logout failed. ${err?.toString()}`
        )
        throw new SamlError('Logout failed', {
          redirectUrl: validateRelayStateUrl(req)?.toString() ?? defaultPageUrl,
          cause: err
        })
      }
    }
  }

  // Returns an Express router for handling SAML-related requests.
  //
  // We support two SAML "bindings", which define how data is passed by the
  // browser to the SP (us) and the IDP.
  // * HTTP redirect: the browser makes a GET request with query parameters
  // * HTTP POST: the browser makes a POST request with URI-encoded form body
  const router = express.Router()
  router.use(sessions.middleware)
  if (citizenCookieSecret) {
    router.use(cookieParser(citizenCookieSecret))
  }
  // Our application directs the browser to this endpoint to start the login
  // flow. We generate a LoginRequest.
  router.get(`/login`, toRequestHandler(login))
  // The IDP makes the browser POST to this callback during login flow, and
  // a SAML LoginResponse is included in the request.
  router.post(
    `/login/callback`,
    urlencodedParser,
    toRequestHandler(loginCallback)
  )
  // Our application directs the browser to one of these endpoints to start
  // the logout flow. We generate a LogoutRequest.
  router.get(`/logout`, toRequestHandler(logout))
  // The IDP makes the browser either GET or POST one of these endpoints in two
  // separate logout flows.
  // 1. SP-initiated logout. In this case the logout flow started from us
  //   (= /auth/saml/logout endpoint), and a SAML LogoutResponse is included
  //   in the request.
  // 2. IDP-initiated logout (= SAML single logout). In this case the logout
  //   flow started from the IDP, and a SAML LogoutRequest is included in the
  //   request.
  router.get(`/logout/callback`, toRequestHandler(logoutCallback))
  router.post(
    `/logout/callback`,
    urlencodedParser,
    toRequestHandler(logoutCallback)
  )

  return {
    router,
    logout
  }
}
