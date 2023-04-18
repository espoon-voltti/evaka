// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Request, Router, urlencoded } from 'express'
import { logDebug } from '../shared/logging'
import passport, { Strategy } from 'passport'
import { EvakaSessionUser } from '../shared/auth'
import { fromCallback } from '../shared/promise-utils'
import { logoutExpress, saveSession } from '../shared/session'
import { assertStringProp, toRequestHandler } from '../shared/express'
import _ from 'lodash'
import { getCitizens } from '../shared/dev-api'
import { createDevSuomiFiStrategy } from './suomi-fi-saml'
import { parseRelayState } from '../shared/saml'

export function createDevSfiRouter(): Router {
  const strategyName = 'suomifi'
  passport.use(strategyName, createDevSuomiFiStrategy())

  const router = Router()
  const root = '/'

  router.get(
    '/login',
    toRequestHandler(async (req, res) => {
      const defaultSsn = '070644-937X'

      const citizens = _.orderBy(
        await getCitizens(),
        [
          ({ ssn }) => defaultSsn === ssn,
          ({ dependantCount }) => dependantCount,
          ({ ssn }) => ssn
        ],
        ['desc', 'desc', 'asc']
      )
      const citizenInputs = citizens
        .map(({ ssn, firstName, lastName, dependantCount }) => {
          if (!ssn) return ''
          const checked = ssn === defaultSsn ? 'checked' : ''
          return `<div><input type="radio" id="${ssn}" name="preset" value="${ssn}" ${checked}/><label for="${ssn}">${firstName} ${lastName} (${dependantCount} huollettavaa)</label></div>`
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
            <h1>Devausympäristön Suomi.fi-kirjautuminen</h1>
            <form action="${formUri}" method="post">
                ${citizenInputs.join('\n')}
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
      await logoutExpress(req, res, 'enduser')
      res.redirect(root)
    })
  )
  return router
}

type ProfileGetter = (userId: string) => Promise<EvakaSessionUser>

export class DevSfiStrategy extends Strategy {
  constructor(private profileGetter: ProfileGetter) {
    super()
  }

  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  authenticate(req: Request, _options?: any): any {
    const shouldRedirect = !req.url.startsWith('/login/callback')

    if (shouldRedirect) {
      return this.redirect(
        `${req.baseUrl}/login?RelayState=${req.query.RelayState}`
      )
    }

    const preset = assertStringProp(req.body, 'preset')

    this.profileGetter(preset)
      .then((user) => this.success(user))
      .catch((err) => this.error(err))
  }

  logout(req: Request, cb: (err: Error | null, url?: string | null) => void) {
    cb(null, null)
  }
}
