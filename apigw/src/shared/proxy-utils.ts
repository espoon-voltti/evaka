// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import expressHttpProxy from 'express-http-proxy'
import type express from 'express'
import { evakaServiceUrl } from './config'
import { createHeaders } from './auth'

interface ProxyOptions {
  path?: string | ((req: express.Request) => string)
}

export function createProxy({ path }: ProxyOptions = {}) {
  return expressHttpProxy(evakaServiceUrl, {
    proxyReqPathResolver: typeof path === 'string' ? () => path : path,
    proxyReqOptDecorator: (proxyReqOpts, srcReq) => {
      const headers = createHeaders(srcReq)
      proxyReqOpts.headers = {
        ...proxyReqOpts.headers,
        ...headers
      }
      return proxyReqOpts
    }
  })
}
