// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import passport, { Strategy } from 'passport'
import { Request, Router, urlencoded } from 'express'
import { EvakaSessionUser } from './index'
import { AsyncRequestHandler, toRequestHandler } from '../express'
import { fromCallback } from '../promise-utils'
import { logoutExpress, saveSession, SessionType } from '../session'
import { parseRelayState } from '../saml'
import { logDebug } from '../logging'

export interface DevAuthRouterOptions {
  root: string
  strategyName: string
  sessionType: SessionType
  verifyUser: (req: Request) => Promise<EvakaSessionUser>
  loginFormHandler: AsyncRequestHandler
}

class DevStrategy extends Strategy {
  constructor(private verifyUser: (req: Request) => Promise<EvakaSessionUser>) {
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

    this.verifyUser(req)
      .then((user) => this.success(user))
      .catch((err) => this.error(err))
  }

  logout(req: Request, cb: (err: Error | null, url?: string | null) => void) {
    cb(null, null)
  }
}

export function createDevAuthRouter({
  root,
  strategyName,
  sessionType,
  verifyUser,
  loginFormHandler
}: DevAuthRouterOptions): Router {
  passport.use(strategyName, new DevStrategy(verifyUser))

  const router = Router()

  router.get('/login', toRequestHandler(loginFormHandler))
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
            await fromCallback<void>((cb) => req.logIn(user, cb))

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
      await logoutExpress(req, res, sessionType)
      res.redirect(root)
    })
  )
  return router
}
