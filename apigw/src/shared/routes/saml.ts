// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import express from 'express'
import passport from 'passport'
import passportSaml from '@node-saml/passport-saml'
import { createLogoutToken, login, logout } from '../auth/index.js'
import { gatewayRole, nodeEnv } from '../config.js'
import { toMiddleware, toRequestHandler } from '../express.js'
import { logAuditEvent, logDebug } from '../logging.js'
import { fromCallback } from '../promise-utils.js'
import { Sessions } from '../session.js'
import { parseDescriptionFromSamlError } from '../saml/error-utils.js'
import type {
  AuthenticateOptions,
  RequestWithUser
} from '@node-saml/passport-saml/lib/types.js'
import { parseRelayState } from '../saml/index.js'

const urlencodedParser = express.urlencoded({ extended: false })

function getDefaultPageUrl(req: express.Request): string {
  switch (gatewayRole) {
    case 'internal':
      return '/employee'
    case 'enduser':
      return '/'
    default:
  }
  if (nodeEnv === 'local') {
    switch (req.headers['host']) {
      case 'localhost:3020':
        return '/employee'
      default:
    }
  }
  return '/'
}

function getRedirectUrl(req: express.Request): string {
  return parseRelayState(req) ?? getDefaultPageUrl(req)
}

export interface SamlEndpointConfig {
  sessions: Sessions
  strategyName: string
  strategy: passportSaml.Strategy
}

function createLoginHandler({
  sessions,
  strategyName
}: SamlEndpointConfig): express.RequestHandler {
  return (req, res, next) => {
    logAuditEvent(
      `evaka.saml.${strategyName}.sign_in_started`,
      req,
      'Login endpoint called'
    )
    const locale = req.query.locale
    const options: AuthenticateOptions = {
      additionalParams: typeof locale === 'string' ? { locale } : {}
    }
    passport.authenticate(
      strategyName,
      options,
      (
        // eslint-disable-next-line @typescript-eslint/no-explicit-any
        err: any,
        user: (Express.User & passportSaml.Profile) | undefined
      ) => {
        if (err || !user) {
          const description =
            parseDescriptionFromSamlError(err, req) ||
            'Could not parse SAML message'

          if (err.message === 'InResponseTo is not valid' && req.user) {
            // When user uses browse back functionality after login we get invalid InResponseTo
            // This will ignore the error
            const redirectUrl = getRedirectUrl(req)
            logDebug(`Redirecting to ${redirectUrl}`, req, { redirectUrl })
            return res.redirect(redirectUrl)
          }

          logAuditEvent(
            `evaka.saml.${strategyName}.sign_in_failed`,
            req,
            `Failed to authenticate user. Description: ${description}. Details: ${err}`
          )
          return res.redirect(`${getDefaultPageUrl(req)}?loginError=true`)
        }
        ;(async () => {
          await login(req, user)
          logAuditEvent(
            `evaka.saml.${strategyName}.sign_in`,
            req,
            'User logged in successfully'
          )

          if (!user.nameID) {
            throw new Error('User unexpectedly missing nameID property')
          }

          // Persist in session to allow custom logic per strategy
          req.session.idpProvider = strategyName
          await sessions.saveLogoutToken(
            req,
            createLogoutToken(user.nameID, user.sessionIndex)
          )

          const redirectUrl = getRedirectUrl(req)
          logDebug(`Redirecting to ${redirectUrl}`, req, { redirectUrl })
          return res.redirect(redirectUrl)
        })().catch((err) => {
          logAuditEvent(
            `evaka.saml.${strategyName}.sign_in_failed`,
            req,
            `Error logging user in. Error: ${err}`
          )
          if (!res.headersSent) {
            res.redirect(`${getDefaultPageUrl(req)}?loginError=true`)
          } else {
            next(err)
          }
        })
      }
    )(req, res, next)
  }
}

function createLogoutHandler({
  sessions,
  strategy,
  strategyName
}: SamlEndpointConfig): express.RequestHandler {
  return toRequestHandler(async (req, res) => {
    logAuditEvent(
      `evaka.saml.${strategyName}.sign_out_requested`,
      req,
      'Logout endpoint called'
    )
    try {
      const redirectUrl = await fromCallback<string | null>((cb) =>
        req.user ? strategy.logout(req as RequestWithUser, cb) : cb(null, null)
      )
      logDebug('Logging user out from passport.', req)
      await logout(sessions, req, res)
      res.redirect(redirectUrl ?? getDefaultPageUrl(req))
    } catch (err) {
      logAuditEvent(
        `evaka.saml.${strategyName}.sign_out_failed`,
        req,
        `Log out failed. Description: Failed before redirecting user to IdP. Error: ${err}.`
      )
      throw err
    }
  })
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
  const { strategyName, strategy } = endpointConfig

  passport.use(strategyName, strategy)

  const loginHandler = createLoginHandler(endpointConfig)
  const logoutHandler = createLogoutHandler(endpointConfig)
  const logoutCallback = toMiddleware(async (req) => {
    logAuditEvent(
      `evaka.saml.${strategyName}.sign_out`,
      req,
      'Logout callback called'
    )
  })

  const router = express.Router()

  // Our application directs the browser to this endpoint to start the login
  // flow. We generate a LoginRequest.
  router.get(`/login`, loginHandler)
  // The IDP makes the browser POST to this callback during login flow, and
  // a SAML LoginResponse is included in the request.
  router.post(`/login/callback`, urlencodedParser, loginHandler)

  // Our application directs the browser to one of these endpoints to start
  // the logout flow. We generate a LogoutRequest.
  router.get(`/logout`, logoutHandler)
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
    logoutCallback,
    passport.authenticate(strategyName),
    (req, res) => res.redirect(getRedirectUrl(req))
  )
  router.post(
    `/logout/callback`,
    urlencodedParser,
    logoutCallback,
    passport.authenticate(strategyName),
    (req, res) => res.redirect(getRedirectUrl(req))
  )

  return router
}
