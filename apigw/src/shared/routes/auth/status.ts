// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { NextFunction, Request, Response, Router } from 'express'
import { csrf, csrfCookie } from '../../middleware/csrf'
import { SessionType } from '../../session'
import { getEmployeeDetails } from '../../service-client'

export default function authStatus(sessionType: SessionType) {
  const router = Router()

  router.get(
    '/auth/status',
    csrf,
    csrfCookie(sessionType),
    (req: Request, res: Response, next: NextFunction) => {
      const user = req.user
      if (user) {
        getEmployeeDetails(req, user.id)
          .then((data) =>
            res
              .status(200)
              .send({ loggedIn: true, user: data, roles: user.roles })
          )
          .catch((error) => next(error))
      } else {
        res.status(200).send({ loggedIn: false })
      }
    }
  )

  return router
}
