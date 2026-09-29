// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import type http from 'node:http'

import express from 'express'
import expressHttpProxy from 'express-http-proxy'
import _ from 'lodash'

import {
  digitransitApiEnabled,
  digitransitApiKey,
  digitransitApiUrl,
  enableDevApi
} from '../shared/config.ts'
import { logError, logWarn } from '../shared/logging.ts'
import { createProxy } from '../shared/proxy-utils.ts'

const router = express.Router()

const forwardedRequestHeaders = [
  'accept',
  'accept-language',
  'content-type',
  'content-length'
]

function digitransitResponseHeaders(
  proxyRes: http.IncomingMessage
): http.OutgoingHttpHeaders {
  return {
    'content-type': 'application/json; charset=utf-8',
    ...(proxyRes.headers['content-encoding'] === 'gzip'
      ? { 'content-encoding': 'gzip' }
      : {})
  }
}

export function createDigitransitProxy(
  apiUrl: string,
  apiKey: string | undefined,
  path: string
): express.RequestHandler {
  const proxy = expressHttpProxy(apiUrl, {
    parseReqBody: false,
    proxyReqPathResolver: (req) => {
      const query = req.url.split('?')[1]
      return path + (query ? '?' + query : '')
    },
    proxyReqOptDecorator: (proxyReqOpts, _srcReq) => {
      proxyReqOpts.headers = {
        ..._.pick(proxyReqOpts.headers, forwardedRequestHeaders),
        'accept-encoding': 'gzip',
        ...(apiKey ? { 'digitransit-subscription-key': apiKey } : {})
      }
      return proxyReqOpts
    },
    userResHeaderDecorator: (
      _headers,
      _userReq,
      userRes,
      _proxyReq,
      proxyRes
    ) => ({
      ...(userRes.locals.headersBeforeProxy as http.OutgoingHttpHeaders),
      ...digitransitResponseHeaders(proxyRes)
    }),
    userResDecorator: (proxyRes, proxyResData) => {
      function parseBody(): unknown {
        if (!Buffer.isBuffer(proxyResData)) {
          return undefined
        }
        const body = proxyResData
        try {
          if (proxyRes.headers['content-type'] === 'application/json') {
            return JSON.parse(body.toString('utf-8'))
          }
          if (proxyRes.headers['content-type']?.startsWith('text/')) {
            return body.toString('utf-8')
          }
        } catch (e: unknown) {
          if (e instanceof Error) {
            logError(
              'Failed to parse Digitransit error body',
              undefined,
              undefined,
              e
            )
          }
          return undefined
        }
      }

      if (proxyRes.statusCode && proxyRes.statusCode >= 400) {
        logWarn(
          `Digitransit API error: ${JSON.stringify({
            statusCode: proxyRes.statusCode,
            headers: {
              'content-type': proxyRes.headers['content-type']
            },
            body: parseBody()
          })}`
        )
      }
      // oxlint-disable-next-line typescript/no-unsafe-return
      return proxyResData
    }
  })
  return (req, res, next) => {
    res.locals.headersBeforeProxy = res.getHeaders()
    proxy(req, res, next)
  }
}

router.get(
  '/autocomplete',
  digitransitApiEnabled
    ? createDigitransitProxy(
        digitransitApiUrl,
        digitransitApiKey,
        '/geocoding/v1/autocomplete'
      )
    : enableDevApi
      ? createProxy({
          getUserHeader: () => undefined,
          path: '/dev-api/digitransit/autocomplete'
        })
      : (_, res) => res.status(404)
)

router.post(
  '/query',
  digitransitApiEnabled
    ? createDigitransitProxy(
        digitransitApiUrl,
        digitransitApiKey,
        '/routing/v2/finland/gtfs/v1'
      )
    : (_, res) => res.status(404)
)

export default router
