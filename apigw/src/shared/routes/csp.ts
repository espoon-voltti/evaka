// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import type express from 'express'

import { logWarn } from '../logging.ts'

export const handleCspReport: express.RequestHandler = (req, res) => {
  logWarn('CSP report received', req, {
    // oxlint-disable-next-line typescript/no-unsafe-assignment
    report: req.body
  })
  res.sendStatus(200)
}
