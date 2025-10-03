// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { randomBytes } from 'node:crypto'

import type express from 'express'

// Generates a random 64-bit tracing ID in hex format
const randomTracingId = () => randomBytes(8).toString('hex')

const tracing: express.RequestHandler = (req, res, next) => {
  const requestIdHeader = req.header('x-request-id')
  if (requestIdHeader) {
    req.traceId = requestIdHeader
    req.spanId = randomTracingId()
  } else {
    const traceId = randomTracingId()
    req.traceId = traceId
    req.spanId = traceId
  }
  next()
}
export default tracing
