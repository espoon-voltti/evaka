// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { OutgoingHttpHeaders } from 'node:http'

import type express from 'express'
import expressHttpProxy from 'express-http-proxy'
import _ from 'lodash'

import { evakaServiceUrl } from './config.js'
import { createServiceRequestHeaders } from './service-client.js'

interface ProxyOptions {
  path?: string | ((req: express.Request) => string)
  url?: string
  getUserHeader: (req: express.Request) => string | undefined
}

export function createProxy({
  path,
  url = evakaServiceUrl,
  getUserHeader
}: ProxyOptions) {
  return expressHttpProxy(url, {
    parseReqBody: false,
    proxyReqPathResolver: typeof path === 'string' ? () => path : path,
    proxyReqOptDecorator: (proxyReqOpts, srcReq) => {
      const originalHeaders = isHeaders(proxyReqOpts.headers)
        ? lowercaseHeaderNames(proxyReqOpts.headers ?? {})
        : {}

      // Remove sensitive headers
      delete originalHeaders['authorization']
      delete originalHeaders['x-user']

      const serviceHeaders = lowercaseHeaderNames(
        createServiceRequestHeaders(srcReq, getUserHeader(srcReq))
      )
      proxyReqOpts.headers = {
        ...originalHeaders,
        ...serviceHeaders
      }
      return proxyReqOpts
    }
  })
}

function isHeaders(
  obj: OutgoingHttpHeaders | readonly string[] | undefined
): obj is OutgoingHttpHeaders | undefined {
  return !Array.isArray(obj)
}

function lowercaseHeaderNames(obj: OutgoingHttpHeaders): OutgoingHttpHeaders {
  return _.mapKeys(obj, (_, key) => key.toLowerCase())
}
