// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import express from 'express'

import { logWarn } from '../logging.js'

export const handleCspReport: express.RequestHandler = (req, res) => {
  logWarn('CSP report received', req, {
    // eslint-disable-next-line @typescript-eslint/no-unsafe-assignment
    report: req.body
  })
  res.sendStatus(200)
}
