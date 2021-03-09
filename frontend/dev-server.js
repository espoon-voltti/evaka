const assert = require('assert')
const webpack = require('webpack')
const devMiddleware = require('webpack-dev-middleware')
const proxy = require('express-http-proxy')

const rawConfigs = require('./webpack.config.js')({ DEV_SERVER: true }, {})
const express = require('express')
const publicPaths = ['/employee/mobile', '/employee', '/']

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
  proxy('http://localhost:3020', {
    proxyReqPathResolver: ({ originalUrl }) => originalUrl
  })
)
app.use(
  '/api/application',
  proxy('http://localhost:3010', {
    proxyReqPathResolver: ({ originalUrl }) => originalUrl
  })
)
for (const publicPath of publicPaths) {
  const ctx = contexts[publicPath] ?? contexts[`${publicPath}/`]
  assert(ctx)
  app.use(ctx.middleware)
  app.get(`${publicPath}/*`, (req, res, next) => {
    req.url = `${publicPath}/index.html`
    next()
  })
  app.get(publicPath, (req, res, next) => {
    req.url = `${publicPath}/index.html`
    next()
  })
  app.get(`${publicPath}/index.html`, ctx.middleware)
}

const port = 9099
app.listen(port, () => {
  console.info(`Dev server started at http://localhost:${port}`)
})
