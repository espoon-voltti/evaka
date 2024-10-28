// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Buffer } from 'node:buffer'

import express from 'express'
import { v4 as uuidv4 } from 'uuid'

// Generates a random 64-bit tracing ID in hex format
const randomTracingId = () =>
  Buffer.from(uuidv4(undefined, Buffer.alloc(16))).toString('hex', 8)

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
