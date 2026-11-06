// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import nock from 'nock'
import { afterEach, beforeEach, describe, expect, test, vi } from 'vitest'

import type { Config } from '../../shared/config.ts'
import { configFromEnv, evakaServiceUrl } from '../../shared/config.ts'
import { logAuditEvent } from '../../shared/logging.ts'
import type {
  EmployeeAuthResponse,
  EmployeeUser
} from '../../shared/service-client.ts'
import { GatewayTester } from '../../shared/test/gateway-tester.ts'

vi.mock('../../shared/logging.ts', async (importOriginal) => {
  const actual =
    await importOriginal<typeof import('../../shared/logging.ts')>()
  return { ...actual, logAuditEvent: vi.fn(actual.logAuditEvent) }
})

const employee: EmployeeUser = {
  id: '8fc11215-6d6a-4fcb-a7fe-e3ac3e1f3d7e',
  firstName: 'Teppo',
  lastName: 'Testaaja',
  globalRoles: ['ADMIN'],
  allScopedRoles: []
}

const details = (
  globalRoles: string[],
  allScopedRoles: string[] = []
): EmployeeAuthResponse => ({
  user: {
    ...employee,
    globalRoles: globalRoles as EmployeeUser['globalRoles'],
    allScopedRoles: allScopedRoles as EmployeeUser['allScopedRoles'],
    accessibleFeatures: {},
    permittedGlobalActions: [],
    startPage: 'APPLICATIONS'
  },
  featureConfig: {}
})

const minutes = (n: number) => n * 60 * 1000

describe('Employee session refresh', () => {
  let tester: GatewayTester
  let now: number
  let unmatchedRequests: string[]
  const onNoMatch = (req: { url?: string }) => {
    if (req.url?.startsWith(evakaServiceUrl)) unmatchedRequests.push(req.url)
  }

  async function start(employeeConfig: Partial<Config['employee']>) {
    const config = configFromEnv()
    tester = await GatewayTester.start(
      {
        ...config,
        employee: {
          ...config.employee,
          roleRefreshIntervalSeconds: 120,
          maxSessionAgeMinutes: undefined,
          ...employeeConfig
        }
      },
      'employee'
    )
    tester.setCsrfHeader = true
    await tester.login(employee)
  }

  function advance(ms: number) {
    now += ms
    vi.setSystemTime(now)
  }

  function expectProxiedWithRoles(
    globalRoles: string[],
    allScopedRoles: string[] = []
  ) {
    tester.nockScope
      .get('/employee/some-api')
      .matchHeader('x-user', (value: string | null) => {
        if (!value) return false
        const user = JSON.parse(value) as {
          globalRoles: string[]
          allScopedRoles: string[]
        }
        return (
          JSON.stringify(user.globalRoles) === JSON.stringify(globalRoles) &&
          JSON.stringify(user.allScopedRoles) === JSON.stringify(allScopedRoles)
        )
      })
      .reply(200)
  }

  async function callApi() {
    return tester.client.get('/api/employee/some-api', {
      validateStatus: () => true
    })
  }

  async function firstRequest() {
    tester.nockScope
      .get(`/system/employee/${employee.id}`)
      .reply(200, details(['ADMIN']))
    expectProxiedWithRoles(['ADMIN'])
    expect((await callApi()).status).toBe(200)
    tester.nockScope.done()
  }

  async function getStatus() {
    const res = await tester.client.get<{ loggedIn: boolean }>(
      '/api/employee/auth/status'
    )
    expect(res.status).toBe(200)
    return res.data
  }

  function expectAuditEvent(eventCode: string) {
    expect(logAuditEvent).toHaveBeenCalledWith(
      eventCode,
      expect.anything(),
      expect.any(String)
    )
  }

  async function stayActiveFor(ms: number) {
    advance(ms)
    tester.nockScope
      .get(`/system/employee/${employee.id}`)
      .reply(200, details(['ADMIN']))
    expectProxiedWithRoles(['ADMIN'])
    expect((await callApi()).status).toBe(200)
    tester.nockScope.done()
  }

  beforeEach(() => {
    now = new Date('2026-11-06T08:00:00Z').getTime()
    vi.useFakeTimers({ toFake: ['Date'] })
    vi.setSystemTime(now)
    unmatchedRequests = []
    vi.mocked(logAuditEvent).mockClear()
    nock.emitter.on('no match', onNoMatch)
  })
  afterEach(async () => {
    nock.emitter.removeListener('no match', onNoMatch)
    expect(unmatchedRequests).toEqual([])
    vi.useRealTimers()
    await tester?.afterEach()
    await tester?.stop()
  })

  test('roles are checked on the first request after login', async () => {
    await start({})

    tester.nockScope
      .get(`/system/employee/${employee.id}`)
      .reply(200, details([]))
    expectProxiedWithRoles([])
    expect((await callApi()).status).toBe(200)
    tester.nockScope.done()
  })

  test('roles are not re-checked within the refresh interval', async () => {
    await start({})

    await firstRequest()
    advance(minutes(1))
    expectProxiedWithRoles(['ADMIN'])
    expect((await callApi()).status).toBe(200)
    tester.nockScope.done()
  })

  test('revoked roles are dropped once the refresh interval has passed', async () => {
    await start({})
    await firstRequest()

    advance(minutes(3))
    tester.nockScope
      .get(`/system/employee/${employee.id}`)
      .reply(200, details([]))
    expectProxiedWithRoles([])
    expect((await callApi()).status).toBe(200)
    tester.nockScope.done()

    advance(minutes(1))
    expectProxiedWithRoles([])
    expect((await callApi()).status).toBe(200)
    tester.nockScope.done()
  })

  test('the session ends when the employee is no longer found', async () => {
    await start({})
    await firstRequest()

    advance(minutes(3))
    tester.nockScope.get(`/system/employee/${employee.id}`).reply(404)
    expect((await callApi()).status).toBe(401)
    tester.nockScope.done()
    expectAuditEvent('evaka.employee.session.employee_not_found')

    expect((await callApi()).status).toBe(401)
    tester.nockScope.done()
  })

  test('the session ends when the employee is deactivated', async () => {
    await start({})
    await firstRequest()

    advance(minutes(3))
    tester.nockScope
      .get(`/system/employee/${employee.id}`)
      .reply(404, { errorCode: 'ACCOUNT_INACTIVE' })
    expect((await callApi()).status).toBe(401)
    tester.nockScope.done()
    expectAuditEvent('evaka.employee.session.employee_inactive')
  })

  test('a backend error keeps the session and the check is retried on the next request', async () => {
    await start({})
    await firstRequest()

    advance(minutes(3))
    tester.nockScope.get(`/system/employee/${employee.id}`).reply(503)
    expectProxiedWithRoles(['ADMIN'])
    expect((await callApi()).status).toBe(200)
    tester.nockScope.done()

    tester.nockScope
      .get(`/system/employee/${employee.id}`)
      .reply(200, details(['ADMIN']))
    expectProxiedWithRoles(['ADMIN'])
    expect((await callApi()).status).toBe(200)
    tester.nockScope.done()
  })

  test('the session ends once it is older than the maximum session age', async () => {
    await start({ maxSessionAgeMinutes: 60 })
    await firstRequest()

    await stayActiveFor(minutes(25))
    await stayActiveFor(minutes(25))

    advance(minutes(25))
    expect((await callApi()).status).toBe(401)
    tester.nockScope.done()
    expectAuditEvent('evaka.employee.session.max_age_reached')
  })

  test('sessions have no maximum age by default', async () => {
    await start({})
    await firstRequest()

    await stayActiveFor(minutes(25))
    await stayActiveFor(minutes(25))
    await stayActiveFor(minutes(25))
    await stayActiveFor(minutes(25))
  })

  test('roles are not re-checked within the refresh interval after a successful check', async () => {
    await start({})
    await firstRequest()

    advance(minutes(3))
    tester.nockScope
      .get(`/system/employee/${employee.id}`)
      .reply(200, details(['ADMIN']))
    expectProxiedWithRoles(['ADMIN'])
    expect((await callApi()).status).toBe(200)
    tester.nockScope.done()

    advance(minutes(1))
    expectProxiedWithRoles(['ADMIN'])
    expect((await callApi()).status).toBe(200)
    tester.nockScope.done()
  })

  test('revoked scoped roles are dropped once the refresh interval has passed', async () => {
    await start({})
    await firstRequest()

    advance(minutes(3))
    tester.nockScope
      .get(`/system/employee/${employee.id}`)
      .reply(200, details(['ADMIN'], ['UNIT_SUPERVISOR']))
    expectProxiedWithRoles(['ADMIN'], ['UNIT_SUPERVISOR'])
    expect((await callApi()).status).toBe(200)
    tester.nockScope.done()

    advance(minutes(3))
    tester.nockScope
      .get(`/system/employee/${employee.id}`)
      .reply(200, details(['ADMIN'], []))
    expectProxiedWithRoles(['ADMIN'], [])
    expect((await callApi()).status).toBe(200)
    tester.nockScope.done()
  })

  test('a role refresh through auth status resets the refresh interval', async () => {
    await start({})
    await firstRequest()

    advance(minutes(1.5))
    tester.nockScope
      .get(`/system/employee/${employee.id}`)
      .reply(200, details([]))
    expect((await getStatus()).loggedIn).toBe(true)
    tester.nockScope.done()

    advance(minutes(1.5))
    expectProxiedWithRoles([])
    expect((await callApi()).status).toBe(200)
    tester.nockScope.done()
  })

  test('a role check through auth status with unchanged roles resets the refresh interval', async () => {
    await start({})
    await firstRequest()

    advance(minutes(1.5))
    tester.nockScope
      .get(`/system/employee/${employee.id}`)
      .reply(200, details(['ADMIN']))
    expect((await getStatus()).loggedIn).toBe(true)
    tester.nockScope.done()

    advance(minutes(1.5))
    expectProxiedWithRoles(['ADMIN'])
    expect((await callApi()).status).toBe(200)
    tester.nockScope.done()
  })

  test('auth status ends the session of a deactivated employee', async () => {
    await start({})
    await firstRequest()

    advance(minutes(1))
    tester.nockScope
      .get(`/system/employee/${employee.id}`)
      .reply(404, { errorCode: 'ACCOUNT_INACTIVE' })
    expect((await getStatus()).loggedIn).toBe(false)
    tester.nockScope.done()

    expect((await callApi()).status).toBe(401)
  })

  test('auth status checks roles once per request after the refresh interval', async () => {
    await start({})
    await firstRequest()

    advance(minutes(3))
    tester.nockScope
      .get(`/system/employee/${employee.id}`)
      .reply(200, details(['ADMIN']))
    expect((await getStatus()).loggedIn).toBe(true)
    tester.nockScope.done()
  })
})
