// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { ErrorRequestHandler } from 'express'

import { includeAllErrorMessages } from '../config.js'
import { InvalidRequest } from '../express.js'
import { logError } from '../logging.js'
import { SamlError } from '../routes/saml.js'

import { InvalidAntiCsrfToken } from './csrf.js'

interface LogResponse {
  message: string | null
  errorCode?: string
}

export const errorHandler: (v: boolean) => ErrorRequestHandler =
  (includeErrorMessage: boolean) => (error, req, res, next) => {
    if (error instanceof InvalidAntiCsrfToken) {
      logError('Anti-CSRF token error', req, undefined, error)
      if (!res.headersSent) {
        res
          .status(403)
          .send({ message: 'Anti-CSRF token error' } as LogResponse)
      }
      return
    }
    if (error instanceof SamlError) {
      if (!error.options?.silent) {
        logError(error.message, req, undefined, error)
      }
      if (res.headersSent) return
      if (error.options?.redirectUrl) {
        res.redirect(error.options.redirectUrl)
      } else {
        res.sendStatus(500)
      }
      return
    }
    if (error instanceof InvalidRequest) {
      const response: LogResponse = {
        message:
          includeErrorMessage || includeAllErrorMessages ? error.message : null
      }
      if (!res.headersSent) {
        res.status(400).json(response)
      }
      return
    }
    // eslint-disable-next-line @typescript-eslint/no-unsafe-member-access
    if (error.response) {
      const response: LogResponse = {
        // eslint-disable-next-line @typescript-eslint/no-unsafe-assignment
        message: includeErrorMessage
          ? // eslint-disable-next-line @typescript-eslint/no-unsafe-member-access
            error.response.data?.message || 'Invalid downstream error response'
          : null,
        // eslint-disable-next-line @typescript-eslint/no-unsafe-member-access,@typescript-eslint/no-unsafe-assignment
        errorCode: error.response.data?.errorCode
      }
      if (!res.headersSent) {
        // eslint-disable-next-line @typescript-eslint/no-unsafe-member-access,@typescript-eslint/no-unsafe-argument
        res.status(error.response.status).json(response)
      }
      return
    }
    return fallbackErrorHandler(error, req, res, next)
  }

export const fallbackErrorHandler: ErrorRequestHandler = (
  error,
  req,
  res,
  _next
) => {
  logError(
    // eslint-disable-next-line @typescript-eslint/no-unsafe-member-access
    `Internal server error: ${error.message || error || 'No error object'}`,
    req,
    undefined,
    // eslint-disable-next-line @typescript-eslint/no-unsafe-argument
    error
  )
  if (!res.headersSent) {
    res
      .status(500)
      .json({ message: 'Internal server error' } satisfies LogResponse)
  }
}
