/**
 * @jest-environment jsdom-global
 */
// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { type JSDOM } from 'jsdom'

import { isProduction, getEnvironment } from './helpers'

declare global {
  var jsdom: JSDOM
}

function withHost(host: string, fn: () => void) {
  const oldHref = window.location.href
  global.jsdom.reconfigure({ url: `http://${host}` })
  try {
    fn()
  } finally {
    global.jsdom.reconfigure({ url: oldHref })
  }
}

describe('helpers', () => {
  afterEach(() => {
    jest.restoreAllMocks()
  })

  describe('isProduction', () => {
    it('returns true when hostname is espoonvarhaiskasvatus.fi', () => {
      withHost('espoonvarhaiskasvatus.fi', () => {
        expect(isProduction()).toBeTruthy()
      })
    })

    it('returns false when hostname is staging.espoonvarhaiskasvatus.fi', () => {
      withHost('staging.espoonvarhaiskasvatus.fi', () => {
        expect(isProduction()).toBeFalsy()
      })
    })
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
})
