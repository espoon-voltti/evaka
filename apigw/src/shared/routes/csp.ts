// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import express from 'express'

import { logWarn } from '../logging.js'

const router = express.Router()

router.post(
  '/csp-report',
  express.json({ type: 'application/csp-report' }),
  (req, res) => {
    logWarn('CSP report received', req, {
      user: req.user,
      // eslint-disable-next-line @typescript-eslint/no-unsafe-assignment
      report: req.body
    })
    res.sendStatus(200)
  }
)

export default router
