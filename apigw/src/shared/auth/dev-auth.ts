// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import passport, { Strategy } from 'passport'
import { Request, Router, urlencoded } from 'express'
import { authenticate, EvakaSessionUser, login, logout } from './index.js'
import { AsyncRequestHandler, toRequestHandler } from '../express.js'
import { LogoutTokens, SessionType } from '../session.js'
import { parseRelayState } from '../saml/index.js'

export interface DevAuthRouterOptions {
  logoutTokens: LogoutTokens
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
    this.verifyUser(req)
      .then((user) => this.success(user))
      .catch((err) => this.error(err))
  }
}

export function createDevAuthRouter({
  logoutTokens,
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
    toRequestHandler(async (req, res) => {
      try {
        const user = await authenticate(strategyName, req, res)
        if (!user) {
          res.redirect(`${root}?loginError=true`)
        } else {
          await login(req, user)
          res.redirect(parseRelayState(req) ?? root)
        }
      } catch (err) {
        if (!res.headersSent) {
          res.redirect(`${root}?loginError=true`)
        }
        throw err
      }
    })
  )

  router.get(
    `/logout`,
    toRequestHandler(async (req, res) => {
      await logout(logoutTokens, sessionType, req, res)
      res.redirect(root)
    })
  )
  return router
}
