// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

const assert = require('assert')

const compression = require('compression')
const express = require('express')
const proxy = require('express-http-proxy')
const { trimEnd } = require('lodash')
const webpack = require('webpack')
const devMiddleware = require('webpack-dev-middleware')

const rawConfigs = require('../webpack.config.js')({}, { mode: 'production' })
const publicPaths = ['/employee/mobile/', '/employee/', '/']

const contexts = Object.fromEntries(
  rawConfigs.map((config) => {
    const compiler = webpack({ ...config, stats: 'minimal' })
    const middleware = devMiddleware(compiler)
    return [config.output.publicPath, { config, compiler, middleware }]
  })
)

const app = express()
app.use(compression())
app.use(
  '/api/internal',
  proxy(process.env.API_PROXY_URL ?? 'http://localhost:3020', {
    proxyReqPathResolver: ({ originalUrl }) => originalUrl,
    limit: '100mb'
  })
)
app.use(
  '/api/application',
  proxy(process.env.API_PROXY_URL ?? 'http://localhost:3010', {
    proxyReqPathResolver: ({ originalUrl }) => originalUrl
  })
)
for (const publicPath of publicPaths) {
  const ctx = contexts[publicPath]
  const pathPrefix = trimEnd(publicPath, '/')
  assert(ctx)
  app.use(ctx.middleware)
  app.get(`${pathPrefix}/*`, (req, res, next) => {
    req.url = `${pathPrefix}/index.html`
    next()
  })
  app.get(pathPrefix, (req, res, next) => {
    req.url = `${pathPrefix}/index.html`
    next()
  })
  app.get(`${pathPrefix}/index.html`, ctx.middleware)
}

const port = 9098
app.listen(port, '0.0.0.0', () => {
  console.info(`Dev server started at http://localhost:${port}`)
})
