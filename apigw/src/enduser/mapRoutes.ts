// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import express from 'express'
import expressHttpProxy from 'express-http-proxy'

import {
  digitransitApiEnabled,
  digitransitApiKey,
  digitransitApiUrl
} from '../shared/config.js'
import { logError, logWarn } from '../shared/logging.js'
import { createProxy } from '../shared/proxy-utils.js'

const router = express.Router()

function createDigitransitProxy(path: string) {
  return expressHttpProxy(digitransitApiUrl, {
    parseReqBody: false,
    proxyReqPathResolver: (req) => {
      const query = req.url.split('?')[1]
      return path + (query ? '?' + query : '')
    },
    proxyReqOptDecorator: (proxyReqOpts, _srcReq) => {
      proxyReqOpts.headers = {
        ...proxyReqOpts.headers,
        ...(digitransitApiKey
          ? { 'digitransit-subscription-key': digitransitApiKey }
          : {})
      }
      return proxyReqOpts
    },
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
      // eslint-disable-next-line @typescript-eslint/no-unsafe-return
      return proxyResData
    }
  })
}

router.get(
  '/map-api/autocomplete',
  digitransitApiEnabled
    ? createDigitransitProxy('/geocoding/v1/autocomplete')
    : createProxy({ path: '/dev-api/digitransit/autocomplete' })
)

router.post(
  '/map-api/query',
  digitransitApiEnabled
    ? createDigitransitProxy('/routing/v1/routers/finland/index/graphql')
    : createProxy({ path: '/dev-api/digitransit/query' })
)

export default router
