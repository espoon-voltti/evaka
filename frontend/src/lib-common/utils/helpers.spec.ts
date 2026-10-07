// @vitest-environment jsdom
// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { afterEach, describe, expect, it, vi } from 'vitest'

import { getEnvironment } from './helpers'

async function importFreshHelpers() {
  vi.resetModules()
  return import('./helpers')
}

function withHost(host: string, fn: () => void) {
  const locationSpy = vi.spyOn(window, 'location', 'get')
  locationSpy.mockReturnValue({
    hostname: host.split(':')[0],
    host,
    href: `http://${host}`
  } as Location)
  try {
    fn()
  } finally {
    locationSpy.mockRestore()
  }
}

describe('helpers', () => {
  afterEach(() => {
    vi.restoreAllMocks()
  })

  describe('getEnvironment', () => {
    it('returns "local" when host is localhost:9093', () => {
      withHost('localhost:9093', () => {
        expect(getEnvironment()).toEqual('local')
      })
    })

    it('returns "staging" when hostname is staging.espoonvarhaiskasvatus.fi', () => {
      withHost('staging.espoonvarhaiskasvatus.fi', () => {
        expect(getEnvironment()).toEqual('staging')
      })
    })

    it('returns "prod" when hostname is espoonvarhaiskasvatus.fi', () => {
      withHost('espoonvarhaiskasvatus.fi', () => {
        expect(getEnvironment()).toEqual('prod')
      })
    })

    it('returns "prod" when hostname is evaka.prod.espoon-voltti.fi', () => {
      withHost('evaka.prod.espoon-voltti.fi', () => {
        expect(getEnvironment()).toEqual('prod')
      })
    })

    it('works with non-predefined environment names', () => {
      const envName = 'somethingtotallysillyandinvalid123'
      withHost(`${envName}.espoonvarhaiskasvatus.fi`, () => {
        expect(getEnvironment()).toEqual(envName)
      })
    })

    it('returns "staging" when hostname is staging.espoonvarhaiskasvatus.fi:443', () => {
      withHost('staging.espoonvarhaiskasvatus.fi:443', () => {
        expect(getEnvironment()).toEqual('staging')
      })
    })
  })

  describe('test config from localStorage', () => {
    afterEach(() => {
      window.localStorage.clear()
      delete window.evaka
    })

    it('uses the stored config when window.evaka is not set', async () => {
      window.localStorage.setItem(
        'evaka.testConfig',
        JSON.stringify({
          automatedTest: true,
          forceStandalone: true,
          mockedTime: '2026-10-07T09:00:00.000Z'
        })
      )

      const { isAutomatedTest } = await importFreshHelpers()

      expect(isAutomatedTest).toBe(true)
      expect(window.evaka?.forceStandalone).toBe(true)
      expect(window.evaka?.mockedTime).toEqual(
        new Date('2026-10-07T09:00:00.000Z')
      )
    })

    it('prefers window.evaka injected before load over the stored config', async () => {
      window.evaka = { automatedTest: false }
      window.localStorage.setItem(
        'evaka.testConfig',
        JSON.stringify({ automatedTest: true })
      )

      const { isAutomatedTest } = await importFreshHelpers()

      expect(isAutomatedTest).toBe(false)
    })

    it('ignores an invalid stored config', async () => {
      window.localStorage.setItem('evaka.testConfig', '{not json')

      const { isAutomatedTest } = await importFreshHelpers()

      expect(isAutomatedTest).toBe(false)
      expect(window.evaka).toBeUndefined()
    })

    it('ignores storage that throws on access', async () => {
      vi.spyOn(Storage.prototype, 'getItem').mockImplementation(() => {
        throw new DOMException('denied', 'SecurityError')
      })

      const { isAutomatedTest } = await importFreshHelpers()

      expect(isAutomatedTest).toBe(false)
    })
  })
})
