// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import type http from 'node:http'
import zlib from 'node:zlib'

import axios from 'axios'
import express from 'express'
import nocache from 'nocache'
import nock from 'nock'
import { afterAll, afterEach, beforeAll, describe, expect, it } from 'vitest'

import { createDigitransitProxy } from '../mapRoutes.ts'

const digitransitUrl = 'http://digitransit.test'
const apiKey = 'test-api-key'

const browserHeaders = {
  cookie: 'evaka.eugw.session=s%3Acitizen; evaka.employee.session=s%3Aemployee',
  authorization: 'Basic dXNlcjpwYXNz',
  'x-real-ip': '203.0.113.7',
  'x-forwarded-for': '203.0.113.7, 10.0.0.1',
  referer: 'https://evaka.test/map',
  'x-evaka-csrf': '1',
  'accept-language': 'fi'
}

const maliciousResponseHeaders = {
  'set-cookie': 'evaka.eugw.session=attacker; Path=/',
  'x-accel-redirect': '/internal_redirect/http://169.254.169.254/',
  location: 'https://attacker.test',
  'cache-control': 'public, max-age=31536000'
}

describe('Digitransit map API proxy', () => {
  let server: http.Server
  let baseUrl: string

  beforeAll(async () => {
    const app = express()
    app.use(nocache())
    app.get(
      '/autocomplete',
      createDigitransitProxy(
        digitransitUrl,
        apiKey,
        '/geocoding/v1/autocomplete'
      )
    )
    app.post(
      '/query',
      createDigitransitProxy(
        digitransitUrl,
        apiKey,
        '/routing/v2/finland/gtfs/v1'
      )
    )
    server = await new Promise((resolve) => {
      const s = app.listen(0, () => resolve(s))
    })
    const address = server.address()
    if (!address || typeof address === 'string')
      throw new Error('Unsupported http server address format')
    baseUrl = `http://localhost:${address.port}`
  })
  afterEach(() => nock.cleanAll())
  afterAll(
    async () =>
      new Promise<void>((resolve, reject) =>
        server.close((err) => (err ? reject(err) : resolve()))
      )
  )

  it('forwards only allowlisted request headers and the API key', async () => {
    let upstreamHeaders: Record<string, unknown> = {}
    const scope = nock(digitransitUrl)
      .get('/geocoding/v1/autocomplete')
      .query({ text: 'foo' })
      .reply(function () {
        upstreamHeaders = this.req.headers
        return [200, { features: [] }]
      })

    const res = await axios.get(`${baseUrl}/autocomplete?text=foo`, {
      headers: browserHeaders
    })

    scope.done()
    expect(res.data).toEqual({ features: [] })
    expect(upstreamHeaders['digitransit-subscription-key']).toBe(apiKey)
    expect(upstreamHeaders['accept-language']).toBe('fi')
    for (const header of [
      'cookie',
      'authorization',
      'x-real-ip',
      'x-forwarded-for',
      'referer',
      'x-evaka-csrf'
    ]) {
      expect(upstreamHeaders[header]).toBeUndefined()
    }
  })

  it('forwards the request body of a routing query', async () => {
    const body = { query: '{ plan { itineraries { duration } } }' }
    const scope = nock(digitransitUrl)
      .post('/routing/v2/finland/gtfs/v1', body)
      .matchHeader('content-type', /^application\/json/)
      .reply(200, { data: {} })

    const res = await axios.post(`${baseUrl}/query`, body, {
      headers: browserHeaders
    })

    scope.done()
    expect(res.data).toEqual({ data: {} })
  })

  it('does not pass through unexpected response headers', async () => {
    const scope = nock(digitransitUrl)
      .get('/geocoding/v1/autocomplete')
      .reply(200, { features: [] }, maliciousResponseHeaders)

    const res = await axios.get(`${baseUrl}/autocomplete`)

    scope.done()
    expect(res.headers['set-cookie']).toBeUndefined()
    expect(res.headers['x-accel-redirect']).toBeUndefined()
    expect(res.headers.location).toBeUndefined()
    expect(res.headers['cache-control']).toContain('no-store')
    expect(res.headers['content-type']).toMatch(/^application\/json/)
  })

  it('always responds with a JSON content type', async () => {
    const scope = nock(digitransitUrl)
      .get('/geocoding/v1/autocomplete')
      .reply(200, '<script>alert(1)</script>', {
        'content-type': 'text/html'
      })

    const res = await axios.get(`${baseUrl}/autocomplete`)

    scope.done()
    expect(res.headers['content-type']).toBe('application/json; charset=utf-8')
  })

  it('passes through a gzipped JSON response', async () => {
    const scope = nock(digitransitUrl)
      .get('/geocoding/v1/autocomplete')
      .matchHeader('accept-encoding', 'gzip')
      .reply(200, zlib.gzipSync(JSON.stringify({ features: [] })), {
        'content-type': 'application/json',
        'content-encoding': 'gzip'
      })

    const res = await axios.get(`${baseUrl}/autocomplete`)

    scope.done()
    expect(res.data).toEqual({ features: [] })
  })
})
