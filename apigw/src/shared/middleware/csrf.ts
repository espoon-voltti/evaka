// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import express from 'express'

const requiresCsrfCheck = (req: express.Request) => {
  switch (req.method) {
    case 'GET':
    case 'HEAD':
    case 'OPTIONS':
      return false
    default:
      return true
  }
}

export class InvalidAntiCsrfToken extends Error {
  constructor() {
    super('Invalid anti-CSRF token')
    this.name = 'InvalidAntiCsrfToken'
  }
}

// Middleware that does CSRF header checks
export const csrf: express.RequestHandler = (req, res, next) => {
  if (requiresCsrfCheck(req)) {
    const sessionToken = req.session?.antiCsrfToken
    const headerToken = req.header('x-evaka-csrf')
    if (!sessionToken || !headerToken || sessionToken !== headerToken)
      return next(new InvalidAntiCsrfToken())
  }
  next()
}
