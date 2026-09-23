// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import http from 'node:http'

import express from 'express'
import expressHttpProxy from 'express-http-proxy'

import { evakaServiceUrl } from '../shared/config.ts'
import { createServiceRequestHeaders } from '../shared/service-client.ts'

/**
 * Routes for the MCP (Model Context Protocol) server that lets AI assistants create test data in
 * non-production environments. Everything here is only mounted when ENABLE_MCP is true.
 *
 * AI clients authenticate with OAuth 2.1 bearer tokens issued by evaka-service, not with sessions
 * or CSRF headers, so this router must be mounted before the CSRF middleware. The client's
 * Authorization header is forwarded to the service under a separate name, because the normal
 * Authorization header carries the apigw -> service JWT.
 */
export function createMcpRouter(): express.Router {
  const router = express.Router()
  router.use('/mcp', cors)
  router.all(
    '/mcp',
    mcpProxy((req) => req.url)
  )
  router.all(
    '/mcp/{*rest}',
    mcpProxy((req) => req.url)
  )

  // OAuth discovery documents must be served from the site root (/.well-known/...). nginx and
  // the vite dev server forward them to /api/.well-known/..., and we pass them on to the MCP
  // controller in evaka-service.
  router.use('/.well-known', cors)
  router.get(
    '/.well-known/oauth-authorization-server{/*rest}',
    mcpProxy((req) => `/mcp${req.url}`)
  )
  router.get(
    '/.well-known/oauth-protected-resource{/*rest}',
    mcpProxy((req) => `/mcp${req.url}`)
  )
  return router
}

const forwardedAuthorizationHeader = 'x-evaka-mcp-authorization'
const proxyAgent = new http.Agent({ keepAlive: true, timeout: 0 })

function mcpProxy(path: (req: express.Request) => string) {
  return expressHttpProxy(evakaServiceUrl, {
    parseReqBody: false,
    proxyReqPathResolver: (req) => path(req),
    proxyReqOptDecorator: (proxyReqOpts, srcReq) => {
      const clientAuthorization = srcReq.get('authorization')
      const headers = Object.fromEntries(
        Object.entries(proxyReqOpts.headers ?? {}).map(([key, value]) => [
          key.toLowerCase(),
          value
        ])
      )
      // Remove sensitive headers that must only be set by apigw itself
      delete headers.authorization
      delete headers['x-user']
      delete headers[forwardedAuthorizationHeader]
      if (clientAuthorization) {
        headers[forwardedAuthorizationHeader] = clientAuthorization
      }
      proxyReqOpts.headers = {
        ...headers,
        ...Object.fromEntries(
          Object.entries(createServiceRequestHeaders(srcReq, undefined)).map(
            ([key, value]) => [key.toLowerCase(), value]
          )
        )
      }
      proxyReqOpts.agent = proxyAgent
      return proxyReqOpts
    }
  })
}

/**
 * Permissive CORS: browser-based MCP clients (e.g. the MCP Inspector) call these endpoints
 * cross-origin. They never use cookies, only bearer tokens, so allowing any origin is safe.
 */
const cors: express.RequestHandler = (req, res, next) => {
  res.setHeader('Access-Control-Allow-Origin', '*')
  res.setHeader('Access-Control-Allow-Methods', 'GET, POST, DELETE, OPTIONS')
  res.setHeader(
    'Access-Control-Allow-Headers',
    'Authorization, Content-Type, Accept, Mcp-Session-Id, MCP-Protocol-Version, Last-Event-ID'
  )
  res.setHeader(
    'Access-Control-Expose-Headers',
    'Mcp-Session-Id, WWW-Authenticate'
  )
  res.setHeader('Access-Control-Max-Age', '600')
  if (req.method === 'OPTIONS') {
    res.sendStatus(204)
    return
  }
  next()
}
