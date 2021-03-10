const { trimEnd } = require('lodash')
const assert = require('assert')
const webpack = require('webpack')
const devMiddleware = require('webpack-dev-middleware')
const proxy = require('express-http-proxy')

const rawConfigs = require('./webpack.config.js')({ DEV_SERVER: true }, {})
const express = require('express')
const publicPaths = ['/employee/mobile/', '/employee/', '/']

const contexts = Object.fromEntries(
  rawConfigs.map((config) => {
    const compiler = webpack({ ...config, stats: 'minimal' })
    const middleware = devMiddleware(compiler)
    return [config.output.publicPath, { config, compiler, middleware }]
  })
)

const app = express()
app.use(
  '/api/internal',
  proxy(process.env.API_PROXY_URL ?? 'http://localhost:3020', {
    proxyReqPathResolver: ({ originalUrl }) => originalUrl
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

const port = 9099
app.listen(port, () => {
  console.info(`Dev server started at http://localhost:${port}`)
})
