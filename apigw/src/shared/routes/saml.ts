// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { randomUUID } from 'node:crypto'
import * as url from 'node:url'

import type { AuthOptions, Profile, SAML } from '@node-saml/node-saml'
import { AxiosError } from 'axios'
import cookieParser, { signedCookie } from 'cookie-parser'
import express from 'express'
import _ from 'lodash'

import { createLogoutToken } from '../auth/index.ts'
import { setDeviceAuthHistoryCookie } from '../device-cookies.ts'
import type { AsyncRequestHandler } from '../express.ts'
import { toRequestHandler } from '../express.ts'
import { logAuditEvent, logDebug, logError } from '../logging.ts'
import {
  parseDescriptionFromSamlError,
  samlErrorSchema
} from '../saml/error-utils.ts'
import type { AuthenticateProfile } from '../saml/index.ts'
import {
  buildRelayStateWithCorrelationToken,
  extractCorrelationToken,
  getRawUnvalidatedRelayState,
  SamlProfileIdSchema,
  SamlSessionSchema,
  validateRelayStateUrl
} from '../saml/index.ts'
import type { LoginCorrelation, Sessions, SessionType } from '../session.ts'

const urlencodedParser = express.urlencoded({ extended: false })

export interface SamlEndpointConfig<T extends SessionType> {
  sessions: Sessions<T>
  saml: SAML
  strategyName: string
  defaultPageUrl: string
  authenticate: AuthenticateProfile
  citizenCookieSecret?: string
  secondaryCookieConfig?: {
    cookieName: string
    cookieSecret: string
  }
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
    citizenCookieSecret,
    secondaryCookieConfig
  } = endpointConfig

  const eventCode = (name: SamlAuditEvent) =>
    `evaka.saml.${strategyName}.${name}`
  const errorRedirectUrl = (err: unknown) => {
    let errorCode: string | undefined = undefined
    if (err instanceof AxiosError) {
      // oxlint-disable-next-line typescript/no-unsafe-member-access,typescript/no-unsafe-call,typescript/no-unsafe-assignment
      errorCode = err.response?.data?.errorCode?.toString() ?? undefined
    }
    return errorCode
      ? `${defaultPageUrl}?loginError=true&errorCode=${encodeURIComponent(errorCode)}`
      : `${defaultPageUrl}?loginError=true`
  }

  // User not found or similar errors return 4xx status code
  const isExpectedDownstreamError = (err: unknown) =>
    err instanceof AxiosError &&
    err.response !== undefined &&
    err.response.status >= 400 &&
    err.response.status < 500

  const validateSamlLoginResponse = async (
    req: express.Request
  ): Promise<Profile> => {
    // oxlint-disable-next-line typescript/no-unsafe-argument
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
        ? // oxlint-disable-next-line typescript/no-unsafe-argument
          await saml.validatePostRequestAsync(req.body)
        : // oxlint-disable-next-line typescript/no-unsafe-argument
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
    // Run cookieParser to get req.cookies, but do not pass cookie secret to get access to raw cookies
    await runMiddleware(cookieParser(), req, res)
    try {
      const relayState = getRawUnvalidatedRelayState(req) ?? ''
      // Only sfi integrations have a co-resident session to correlate with, and
      // only they can leave a rejected login behind that needs one.
      let correlationToken: string | undefined
      if (secondaryCookieConfig) {
        const ownSid = sessions.sessionIdFromCookie(
          req.cookies[sessions.cookieName] as string | undefined
        )
        const secondaryCookie = req.cookies[
          secondaryCookieConfig.cookieName
        ] as string | undefined
        const secondarySid = secondaryCookie
          ? signedCookie(secondaryCookie, secondaryCookieConfig.cookieSecret) ||
            undefined
          : undefined
        if (ownSid || secondarySid) {
          correlationToken = randomUUID()
          await sessions.saveLoginCorrelation(correlationToken, {
            ownSid,
            secondarySid
          })
        }
      }
      // The RelayState (redirect URL + correlation token) is sent unvalidated to
      // the IdP here; it only matters in the login callback, where it is validated.
      const idpLoginUrl = await saml.getAuthorizeUrlAsync(
        buildRelayStateWithCorrelationToken(relayState, correlationToken),
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
  const persistRejectedSfiLogin = async (
    profile: Profile,
    correlation: LoginCorrelation
  ): Promise<void> => {
    const samlSession = SamlSessionSchema.safeParse(profile)
    if (!samlSession.success) return
    const ids = [correlation.ownSid, correlation.secondarySid].filter(
      (id): id is string => !!id
    )
    if (ids.length === 0) return
    await sessions.saveRejectedSfiLogin(ids, samlSession.data)
  }

  const loginCallback: AsyncRequestHandler = async (req, res) => {
    logAuditEvent(eventCode('sign_in'), req, 'Login callback endpoint called')
    let profile: Profile
    try {
      profile = await validateSamlLoginResponse(req)
    } catch (err) {
      if (
        err instanceof Error &&
        (err.message === 'InResponseTo is not valid' ||
          err.message === 'InResponseTo is missing from response')
      )
        // These errors can happen for example when the user browses back to
        // the login callback after login, or when an unsolicited login
        // response is received
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
    const correlationToken = extractCorrelationToken(req)
    const correlation = correlationToken
      ? await sessions.consumeLoginCorrelation(correlationToken)
      : {}
    try {
      const user = await authenticate(req, profile)
      await sessions.login(req, user, correlation.secondarySid)
      logAuditEvent(eventCode('sign_in'), req, 'User logged in successfully')

      // Set device cookie for citizen authentication
      if (citizenCookieSecret) {
        setDeviceAuthHistoryCookie(res, user.id, citizenCookieSecret)
      }

      // Persist in session to allow custom logic per strategy
      req.session.idpProvider = strategyName
      await sessions.saveLogoutToken(req, createLogoutToken(profile))

      const redirectUrl =
        validateRelayStateUrl(req)?.toString() ?? defaultPageUrl
      logDebug(`Redirecting to ${redirectUrl}`, req, { redirectUrl })
      return res.redirect(redirectUrl)
    } catch (err) {
      try {
        await persistRejectedSfiLogin(profile, correlation)
      } catch (persistErr) {
        logError(
          'Failed to persist rejected sfi login',
          req,
          undefined,
          persistErr instanceof Error ? persistErr : undefined
        )
      }
      logAuditEvent(
        eventCode('sign_in_failed'),
        req,
        `Error logging user in. ${err?.toString()}`
      )
      throw new SamlError('Login failed', {
        redirectUrl: errorRedirectUrl(err),
        cause: err,
        silent: isExpectedDownstreamError(err)
      })
    }
  }

  const runMiddleware = (
    middleware: express.RequestHandler,
    req: express.Request,
    res: express.Response
  ): Promise<void> =>
    new Promise((resolve, reject) => {
      middleware(req, res, (err?: unknown) =>
        err
          ? reject(
              err instanceof Error
                ? err
                : new Error('Middleware error', { cause: err })
            )
          : resolve()
      )
    })

  const logout: AsyncRequestHandler = async (req, res) => {
    logAuditEvent(
      eventCode('sign_out_requested'),
      req,
      'Logout endpoint called'
    )
    try {
      const user = sessions.getUser(req)
      let samlSession = SamlSessionSchema.safeParse(user)
      let url: string
      if (secondaryCookieConfig) {
        const secondaryUser = await sessions.getSecondaryUserIfNewer(req)
        if (secondaryUser) {
          samlSession = SamlSessionSchema.safeParse(secondaryUser)
        }
      }
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
      if (
        err instanceof Error &&
        (err.message === 'InResponseTo is not valid' ||
          err.message === 'InResponseTo is missing from response' ||
          err.message.startsWith('Bad status code:'))
      ) {
        throw new SamlError('Logout failed', {
          redirectUrl: validateRelayStateUrl(req)?.toString() ?? defaultPageUrl,
          cause: err,
          // just ignore without logging to reduce noise in logs
          silent: true
        })
      }

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
