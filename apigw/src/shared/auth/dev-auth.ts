// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { AxiosError } from 'axios'
import type { Request } from 'express'
import express from 'express'

import { AsyncRequestHandler, toRequestHandler } from '../express.js'
import { parseRelayState } from '../saml/index.js'
import { Sessions } from '../session.js'

import { EvakaSessionUser, login, logout } from './index.js'

export interface DevAuthRouterOptions {
  sessions: Sessions
  root: string
  verifyUser: (req: Request) => Promise<EvakaSessionUser>
  loginFormHandler: AsyncRequestHandler
}

export function createDevAuthRouter({
  sessions,
  root,
  verifyUser,
  loginFormHandler
}: DevAuthRouterOptions): express.Router {
  const router = express.Router()

  router.get('/login', toRequestHandler(loginFormHandler))
  router.post(
    `/login/callback`,
    express.urlencoded({ extended: false }), // needed to parse the POSTed form
    toRequestHandler(async (req, res) => {
      try {
        const user = await verifyUser(req)
        if (!user) {
          res.redirect(`${root}?loginError=true`)
        } else {
          await login(req, user)
          res.redirect(parseRelayState(req) ?? root)
        }
      } catch (err) {
        if (!res.headersSent) {
          // eslint-disable-next-line @typescript-eslint/no-unsafe-member-access
          if (err instanceof AxiosError && err.response?.data?.errorCode) {
            res.redirect(
              // eslint-disable-next-line @typescript-eslint/no-unsafe-member-access
              `${root}?loginError=true&errorCode=${err.response.data.errorCode}`
            )
          } else {
            res.redirect(`${root}?loginError=true`)
          }
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
