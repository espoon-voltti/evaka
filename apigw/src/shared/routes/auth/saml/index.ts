// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import express, { Router } from 'express'
import passport from 'passport'
import { urlencoded } from 'body-parser'
import { logAuditEvent, logDebug } from '../../../logging'
import { eadMock, gatewayRole, nodeEnv } from '../../../config'
import { parseDescriptionFromSamlError } from './error-utils'
import { SamlEndpointConfig, SamlUser } from './types'
import { toMiddleware, toRequestHandler } from '../../../express'
import { logoutExpress, saveLogoutToken } from '../../../session'
import { fromCallback } from '../../../promise-utils'
import { client } from '../../../service-client'

const urlencodedParser = urlencoded({ extended: false })

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
  const relayState = req.body.RelayState || req.query.RelayState
  if (typeof relayState === 'string') {
    try {
      // Trust relayState only if it's a relative URI
      const dummyOrigin = 'http://evaka'
      const url = new URL(relayState, dummyOrigin)
      if (url.origin === dummyOrigin) {
        return `${url.pathname}${url.search}${url.hash}`
      }
    } catch (e) {
      // fall back to default page
    }
  }
  return getDefaultPageUrl(req)
}

function createLoginHandler({
  strategyName,
  sessionType
}: SamlEndpointConfig): express.RequestHandler {
  return (req, res, next) => {
    logAuditEvent(
      `evaka.saml.${strategyName}.sign_in_started`,
      req,
      'Login endpoint called'
    )
    passport.authenticate(
      strategyName,
      // eslint-disable-next-line @typescript-eslint/no-explicit-any
      (err: any, user: SamlUser | undefined) => {
        if (err || !user) {
          const description =
            parseDescriptionFromSamlError(err) || 'Could not parse SAML error.'
          logAuditEvent(
            `evaka.saml.${strategyName}.sign_in_failed`,
            req,
            `Error authenticating user. Description: ${description}. Error: ${err}`
          )
          return res.redirect(`${getDefaultPageUrl(req)}?loginError=true`)
        }
        ;(async () => {
          if (req.session) {
            const session = req.session
            await fromCallback<void>((cb) => session.regenerate(cb))
          }
          await fromCallback<void>((cb) => req.logIn(user, cb))
          logAuditEvent(
            `evaka.saml.${strategyName}.sign_in`,
            req,
            'User logged in successfully'
          )
          await saveLogoutToken(req, res, sessionType)
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
  strategy,
  strategyName,
  sessionType
}: SamlEndpointConfig): express.RequestHandler {
  return toRequestHandler(async (req, res) => {
    logAuditEvent(
      `evaka.saml.${strategyName}.sign_out_requested`,
      req,
      'Logout endpoint called'
    )
    if ('logout' in strategy && req.user) {
      try {
        const redirectUrl = await fromCallback<string>((cb) =>
          strategy.logout(req, cb)
        )
        logDebug('Logging user out from passport.')
        await logoutExpress(req, res, sessionType)
        return res.redirect(redirectUrl)
      } catch (err) {
        const description =
          parseDescriptionFromSamlError(err) || 'Could not parse SAML error.'
        logAuditEvent(
          `evaka.saml.${strategyName}.sign_out_failed`,
          req,
          `Log out failed. Description: ${description}. Error: ${err}.`
        )
        throw err
      }
    } else {
      logAuditEvent(
        `evaka.saml.${strategyName}.sign_out`,
        req,
        'User signed out'
      )
      logDebug('Logging user out from passport.')
      await logoutExpress(req, res, sessionType)
      res.redirect(getRedirectUrl(req))
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
export default function createSamlRouter(config: SamlEndpointConfig): Router {
  const { strategyName, strategy } = config

  passport.use(strategyName, strategy)

  const loginHandler = createLoginHandler(config)
  const logoutHandler = createLogoutHandler(config)
  const logoutCallback = toMiddleware(async (req, res) => {
    logAuditEvent(
      `evaka.saml.${strategyName}.sign_out`,
      req,
      'Logout callback called'
    )
    await logoutExpress(req, res, config.sessionType)
  })

  const router = Router()

  // Our application directs the browser to this endpoint to start the login
  // flow. We generate a LoginRequest.
  router.get('/auth/saml/login', loginHandler)
  // The IDP makes the browser POST to this callback during login flow, and
  // a SAML LoginResponse is included in the request.
  router.post('/auth/saml/login/callback', urlencodedParser, loginHandler)

  if (eadMock) {
    router.get('/dev-auth/login', (req, res) => {
      client.get('/dev-api/employee').then((res2) => {
        res.contentType('text/html').send(`
        <html>
        <body>
            <h1>Devausympäristön AD-kirjautuminen</h1>
            <form action="${req.baseUrl}/auth/saml/login/callback?RelayState=${
          req.query.RelayState
        }" method="post">
                ${res2.data.map(
                  (e: {
                    externalId: string
                    firstName: string
                    lastName: string
                  }) => {
                    const [_, aad] = e.externalId.split(':')
                    return `<input type="radio" id="${aad}" name="preset" value="${aad}" /><label for="${aad}">${e.firstName} ${e.lastName}</label><br/>`
                  }
                )}
                <input type="radio" id="custom" name="preset" value="custom" checked/><label for="custom">Custom (täytä tiedot alle)</label><br/>
                <br/>
                <h2>Custom</h2>
                <label for="aad">AAD: </label>
                <input id="aad-input" name="aad" value="cf5bcd6e-3d0e-4d8e-84a0-5ae2e4e65034" required
                    pattern="[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}"/>
                <br/>
                <label for="firstName">Etunimi: </label>
                <input name="firstName" value="Seppo"/>
                <br/>
                <label for="lastName">Sukunimi: </label>
                <input name="lastName" value="Sorsa"/>
                <br/>
                <label for="email">Email: </label>
                <input name="email" value="seppo.sorsa@espoo.fi"/>
                <br/><br/>
                <label for="roles">Roolit:</label><br/>
                <input id="evaka-espoo-officeholder" type="checkbox" name="roles" value="SERVICE_WORKER" checked /><label for="evaka-espoo-officeholder">Palveluohjaaja</label><br/>
                <input id="evaka-espoo-financeadmin" type="checkbox" name="roles" value="FINANCE_ADMIN" checked /><label for="evaka-espoo-financeadmin">Laskutus</label><br/>
                <input id="evaka-espoo-director" type="checkbox" name="roles" value="DIRECTOR" /><label for="evaka-espoo-director">Raportointi (director)</label><br/>
                <input id="evaka-espoo-admin" type="checkbox" name="roles" value="ADMIN" /><label for="evaka-espoo-admin">Pääkäyttäjä</label><br/>
                <br/>
                <button type="submit">Kirjaudu</button>
            </form>
        </body>
        </html>
        `)
      })
    })
  }

  // Our application directs the browser to one of these endpoints to start
  // the logout flow. We generate a LogoutRequest.
  router.get('/auth/saml/logout', logoutHandler)
  // The IDP makes the browser either GET or POST one of these endpoints in two
  // separate logout flows.
  // 1. SP-initiated logout. In this case the logout flow started from us
  //   (= /auth/saml/logout endpoint), and a SAML LogoutResponse is included
  //   in the request.
  // 2. IDP-initiated logout (= SAML single logout). In this case the logout
  //   flow started from the IDP, and a SAML LogoutRequest is included in the
  //   request.
  router.get(
    '/auth/saml/logout/callback',
    logoutCallback,
    passport.authenticate(strategyName),
    (req, res) => res.redirect(getRedirectUrl(req))
  )
  router.post(
    '/auth/saml/logout/callback',
    urlencodedParser,
    logoutCallback,
    passport.authenticate(strategyName),
    (req, res) => res.redirect(getRedirectUrl(req))
  )

  return router
}
