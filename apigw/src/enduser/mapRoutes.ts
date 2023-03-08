// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Router } from 'express'
import {
  digitransitApiEnabled,
  digitransitApiKey,
  digitransitApiUrl
} from '../shared/config'
import expressHttpProxy from 'express-http-proxy'
import { createProxy } from '../shared/proxy-utils'

const router = Router()

function createDigitransitProxy(path: string) {
  return expressHttpProxy(digitransitApiUrl, {
    proxyReqPathResolver: () => path,
    proxyReqOptDecorator: (proxyReqOpts) => {
      proxyReqOpts.headers = {
        ...proxyReqOpts.headers,
        ...(digitransitApiKey
          ? { 'digitransit-subscription-key': digitransitApiKey }
          : {})
      }
      return proxyReqOpts
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
