// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import type express from 'express'

// Configures the application to trust evaka AWS environment reverse proxies
// This makes the following request properties to be based on the original
// request coming from the Internet:
// * `req.ip`: original client IP
// * `req.ips`: entire IP chain including all proxies
// * `req.protocol`: original protocol (https)
// * `req.hostname`: original hostname
// * `req.secure`: is original request https (true)
export function trustReverseProxy(app: express.Application) {
  app.set('trust proxy', 3) // private ALB, evaka-proxy nginx, public ALB
  app.use((req, res, next) => {
    if ('x-original-forwarded-proto' in req.headers) {
      req.headers['x-forwarded-proto'] =
        req.headers['x-original-forwarded-proto']
    }
    if ('x-original-forwarded-port' in req.headers) {
      req.headers['x-forwarded-port'] = req.headers['x-original-forwarded-port']
    }
    next()
  })
}
