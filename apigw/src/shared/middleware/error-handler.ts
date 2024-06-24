// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { ErrorRequestHandler } from 'express'

import { debug } from '../config.js'
import { InvalidRequest } from '../express.js'
import { logError } from '../logging.js'

import { InvalidAntiCsrfToken } from './csrf.js'

interface LogResponse {
  message: string | null
  errorCode?: string
}

export const errorHandler: (v: boolean) => ErrorRequestHandler =
  (includeErrorMessage: boolean) => (error, req, res, next) => {
    if (error instanceof InvalidAntiCsrfToken) {
      logError('Anti-CSRF token error', req, error)
      if (!res.headersSent) {
        res
          .status(403)
          .send({ message: 'Anti-CSRF token error' } as LogResponse)
      }
      return
    }
    if (error instanceof InvalidRequest) {
      const response: LogResponse = {
        message: includeErrorMessage || debug ? error.message : null
      }
      if (!res.headersSent) {
        res.status(400).json(response)
      }
      return
    }
    if (error.response) {
      const response: LogResponse = {
        message: includeErrorMessage
          ? error.response.data?.message || 'Invalid downstream error response'
          : null,
        errorCode: error.response.data?.errorCode
      }
      if (!res.headersSent) {
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
    `Internal server error: ${error.message || error || 'No error object'}`,
    req,
    undefined,
    error
  )
  if (!res.headersSent) {
    res
      .status(500)
      .json({ message: 'Internal server error' } satisfies LogResponse)
  }
}
