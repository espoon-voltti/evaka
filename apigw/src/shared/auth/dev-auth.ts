// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import passport, { Strategy } from 'passport'
import express from 'express'
import type { Request } from 'express'
import { authenticate, EvakaSessionUser, login, logout } from './index.js'
import { AsyncRequestHandler, toRequestHandler } from '../express.js'
import { Sessions } from '../session.js'
import { parseRelayState } from '../saml/index.js'

export interface DevAuthRouterOptions {
  sessions: Sessions
  root: string
  strategyName: string
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
  sessions,
  root,
  strategyName,
  verifyUser,
  loginFormHandler
}: DevAuthRouterOptions): express.Router {
  passport.use(strategyName, new DevStrategy(verifyUser))

  const router = express.Router()

  router.get('/login', toRequestHandler(loginFormHandler))
  router.post(
    `/login/callback`,
    express.urlencoded({ extended: false }), // needed to parse the POSTed form
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
      await logout(sessions, req, res)
      res.redirect(root)
    })
  )
  return router
}
