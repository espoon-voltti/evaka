// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import http from 'http'

import axios, {
  AxiosInstance,
  AxiosResponse,
  InternalAxiosRequestConfig
} from 'axios'
import express from 'express'
import nock from 'nock'
import { Cookie, CookieJar } from 'tough-cookie'

import { apiRouter } from '../../app.js'
import { Config, evakaServiceUrl } from '../config.js'
import { CitizenUser, EmployeeUser } from '../service-client.js'
import { sessionCookie, SessionType } from '../session.js'

import { MockRedisClient } from './mock-redis-client.js'

export class GatewayTester {
  public readonly client: AxiosInstance
  public readonly cookies: CookieJar
  public readonly nockScope: nock.Scope

  private readonly baseUrl: string
  public setCsrfHeader = false

  private constructor(
    private readonly server: http.Server,
    public readonly sessionType: SessionType
  ) {
    const address = server.address()
    if (!address || typeof address === 'string')
      throw new Error('Unsupported http server address format')
    this.cookies = new CookieJar()
    this.baseUrl = `http://localhost:${address.port}`
    this.client = axios.create({ baseURL: this.baseUrl })
    this.client.interceptors.request.use(async (config) =>
      includeCookiesInRequest(this.baseUrl, this.cookies, config)
    )
    this.client.interceptors.request.use((config) => {
      if (this.setCsrfHeader) {
        config.headers.set('x-evaka-csrf', '1')
      }
      return config
    })
    this.client.interceptors.response.use((res) =>
      storeCookiesFromResponse(this.baseUrl, this.cookies, res)
    )
    this.nockScope = nock(evakaServiceUrl)
  }

  private async findCookie(key: string): Promise<Cookie | undefined> {
    for (const cookie of await this.cookies.getCookies(this.baseUrl)) {
      if (cookie.key === key) return cookie
    }
    return undefined
  }

  public async setCookie(cookie: Cookie): Promise<Cookie> {
    // Cookie domain must be cleared before setting to avoid issue with "localhost"
    // being in the public suffix list (and therefore denied by setCookie())
    cookie.domain = null
    const cookieString = cookie.cookieString()
    return await this.cookies.setCookie(cookieString, this.baseUrl, {
      http: cookie.httpOnly,
      secure: cookie.secure,
      now: cookie.creation ?? undefined,
      sameSiteContext: cookie.sameSite
    })
  }

  public async getCookie(key: string): Promise<Cookie | undefined> {
    return await this.findCookie(key)
  }

  public async expireSession(): Promise<void> {
    const cookie = await this.findCookie(sessionCookie(this.sessionType))
    cookie?.setExpires(new Date(0))
  }

  public async afterEach(): Promise<void> {
    nock.cleanAll()
    await this.cookies.removeAllCookies()
    this.setCsrfHeader = false
  }

  public async stop(): Promise<void> {
    return new Promise((resolve, reject) =>
      this.server.close((err) => (err ? reject(err) : resolve()))
    )
  }

  public async login(
    user: CitizenUser | EmployeeUser,
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    postData?: any
  ): Promise<void> {
    if (this.sessionType === 'employee') {
      // eslint-disable-next-line @typescript-eslint/no-unsafe-assignment
      postData =
        postData !== undefined
          ? postData
          : {
              preset: JSON.stringify({
                externalId: 'dummy',
                firstName: '',
                lastName: '',
                email: ''
              })
            }
      this.nockScope.post('/system/employee-login').reply(200, user)
      await this.client.post(
        '/api/employee/auth/ad/login/callback',
        // eslint-disable-next-line @typescript-eslint/no-unsafe-argument
        new URLSearchParams(postData),
        {
          maxRedirects: 0,
          validateStatus: (status) => status >= 200 && status <= 302
        }
      )
      this.nockScope.done()
    } else {
      // eslint-disable-next-line @typescript-eslint/no-unsafe-assignment
      postData = postData !== undefined ? postData : { preset: 'dummy' }
      this.nockScope.post('/system/citizen-login').reply(200, user)
      await this.client.post(
        '/api/citizen/auth/sfi/login/callback',
        // eslint-disable-next-line @typescript-eslint/no-unsafe-argument
        new URLSearchParams(postData),
        {
          maxRedirects: 0,
          validateStatus: (status) => status >= 200 && status <= 302
        }
      )
      this.nockScope.done()
    }
  }

  public static async start(
    config: Config,
    sessionType: SessionType
  ): Promise<GatewayTester> {
    const app = express()
    const redisClient = new MockRedisClient()
    app.use('/api', apiRouter(config, redisClient))
    return new Promise((resolve) => {
      const server = app.listen(() => {
        resolve(new GatewayTester(server, sessionType))
      })
    })
  }
}

function parseCookies(res: AxiosResponse): Cookie[] {
  const header = res.headers['set-cookie']
  if (!header) return []
  const parsed = Array.isArray(header)
    ? header.map((h) => Cookie.parse(h))
    : [Cookie.parse(header)]
  return parsed.filter((x: Cookie | undefined): x is Cookie => !!x)
}

async function includeCookiesInRequest(
  baseUrl: string,
  cookies: CookieJar,
  config: InternalAxiosRequestConfig
): Promise<InternalAxiosRequestConfig> {
  const url = baseUrl + (config.url ?? '')
  config.headers.set('Cookie', await cookies.getCookieString(url))
  return config
}

async function storeCookiesFromResponse(
  baseUrl: string,
  cookies: CookieJar,
  res: AxiosResponse
): Promise<AxiosResponse> {
  const url = baseUrl + (res.config.url ?? '')
  for (const cookie of parseCookies(res)) {
    await cookies.setCookie(cookie, url)
  }
  return res
}
