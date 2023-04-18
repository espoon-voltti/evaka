// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Router, urlencoded } from 'express'
import { logDebug } from '../shared/logging'
import passport from 'passport'
import { EvakaSessionUser } from '../shared/auth'
import { fromCallback } from '../shared/promise-utils'
import { logoutExpress, saveSession } from '../shared/session'
import { toRequestHandler } from '../shared/express'
import _ from 'lodash'
import { getEmployees } from '../shared/dev-api'
import { parseRelayState } from '../shared/auth/saml'
import { Config } from '../shared/config'
import { createDevAdStrategy } from '../shared/auth/ad-saml'

export function createDevAdRouter(config: Config): Router {
  const strategyName = 'ad'
  passport.use(strategyName, createDevAdStrategy(config.ad))

  const router = Router()
  const root = '/employee'

  router.get(
    '/login',
    toRequestHandler(async (req, res) => {
      const employees = _.sortBy(await getEmployees(), ({ id }) => id)
      const employeeInputs = employees
        .map(({ externalId, firstName, lastName }) => {
          if (!externalId) return ''
          const [_, aad] = externalId.split(':')
          return `<div><input type="radio" id="${aad}" name="preset" value="${aad}" /><label for="${aad}">${firstName} ${lastName}</label></div>`
        })
        .filter((line) => !!line)

      const formQuery =
        typeof req.query.RelayState === 'string'
          ? `?RelayState=${encodeURIComponent(req.query.RelayState)}`
          : ''
      const formUri = `${req.baseUrl}/login/callback${formQuery}`

      res.contentType('text/html').send(`
          <html>
          <body>
            <h1>Devausympäristön AD-kirjautuminen</h1>
            <form action="${formUri}" method="post">
                ${employeeInputs.join('\n')}
                <div style="margin-top: 20px">
                  <input type="radio" id="custom" name="preset" value="custom" checked/><label for="custom">Custom (täytä tiedot alle)</label>
                </div>
                <h2>Custom</h2>
                <label for="aad">AAD: </label>
                <input id="aad-input" name="aad" value="cf5bcd6e-3d0e-4d8e-84a0-5ae2e4e65034" required
                    pattern="[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}"/>
                <div>
                  <label for="firstName">Etunimi: </label>
                  <input name="firstName" value="Seppo"/>
                </div>
                <div>
                  <label for="lastName">Sukunimi: </label>
                  <input name="lastName" value="Sorsa"/>
                </div>
                <div>
                  <label for="email">Email: </label>
                  <input name="email" value="seppo.sorsa@espoo.fi"/>
                </div>
                <div>
                  <label for="roles">Roolit:</label><br>
                  <input id="evaka-espoo-officeholder" type="checkbox" name="roles" value="SERVICE_WORKER" checked /><label for="evaka-espoo-officeholder">Palveluohjaaja</label><br>
                  <input id="evaka-espoo-financeadmin" type="checkbox" name="roles" value="FINANCE_ADMIN" checked /><label for="evaka-espoo-financeadmin">Laskutus</label><br>
                  <input id="evaka-espoo-director" type="checkbox" name="roles" value="DIRECTOR" /><label for="evaka-espoo-director">Raportointi (director)</label><br>
                  <input id="evaka-espoo-admin" type="checkbox" name="roles" value="ADMIN" /><label for="evaka-espoo-admin">Pääkäyttäjä</label><br>
                </div>
                <div style="margin-top: 20px">
                  <button type="submit">Kirjaudu</button>
                </div>
            </form>
          </body>
          </html>
        `)
    })
  )

  router.post(
    `/login/callback`,
    urlencoded({ extended: false }), // needed to parse the POSTed form
    (req, res, next) => {
      passport.authenticate(
        strategyName,
        (
          // eslint-disable-next-line @typescript-eslint/no-explicit-any
          err: any,
          user: EvakaSessionUser | undefined
        ) => {
          if (err || !user) {
            return res.redirect(`${root}?loginError=true`)
          }
          ;(async () => {
            if (req.session) {
              const session = req.session
              await fromCallback<void>((cb) => session.regenerate(cb))
            }
            await fromCallback<void>((cb) => req.logIn(user, cb))
            await saveSession(req)

            return res.redirect(parseRelayState(req) ?? root)
          })().catch((err) => {
            if (!res.headersSent) {
              res.redirect(`${root}?loginError=true`)
            } else {
              next(err)
            }
          })
        }
      )(req, res, next)
    }
  )

  router.get(
    `/logout`,
    toRequestHandler(async (req, res) => {
      logDebug('Logging user out from passport.', req)
      await logoutExpress(req, res, 'employee')
      res.redirect(root)
    })
  )
  return router
}
