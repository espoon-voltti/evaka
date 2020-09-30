// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { isProduction, getEnvironment } from './helpers'

/* eslint-disable @typescript-eslint/no-explicit-any */

function defineWindowLocation(host: string) {
  // eslint-disable-next-line @typescript-eslint/no-unsafe-assignment,@typescript-eslint/no-unsafe-member-access
  ;(global as any).window = Object.create(window)
  Object.defineProperty(window, 'location', { value: { host }, writable: true })
}

describe('helpers', () => {
  describe('isProduction', () => {
    it('returns true when hostname is espoonvarhaiskasvatus.fi', () => {
      defineWindowLocation('espoonvarhaiskasvatus.fi')
      expect(isProduction()).toBeTruthy()
    })

    it('returns false when hostname is staging.espoonvarhaiskasvatus.fi', () => {
      defineWindowLocation('staging.espoonvarhaiskasvatus.fi')
      expect(isProduction()).toBeFalsy()
    })
  })

  describe('getEnvironment', () => {
    it('returns "local" when host is localhost:9093', () => {
      defineWindowLocation('localhost:9093')
      expect(getEnvironment()).toEqual('local')
    })

    it('returns "staging" when hostname is staging.espoonvarhaiskasvatus.fi', () => {
      defineWindowLocation('staging.espoonvarhaiskasvatus.fi')
      expect(getEnvironment()).toEqual('staging')
    })

    it('returns "prod" when hostname is espoonvarhaiskasvatus.fi', () => {
      defineWindowLocation('espoonvarhaiskasvatus.fi')
      expect(getEnvironment()).toEqual('prod')
    })

    it('returns "prod" when hostname is evaka.prod.espoon-voltti.fi', () => {
      defineWindowLocation('evaka.prod.espoon-voltti.fi')
      expect(getEnvironment()).toEqual('prod')
    })

    it('works with non-predefined environment names', () => {
      const envName = 'somethingtotallysillyandinvalid123'
      defineWindowLocation(`${envName}.espoonvarhaiskasvatus.fi`)
      expect(getEnvironment()).toEqual(envName)
    })

    it('returns "staging" when hostname is staging.espoonvarhaiskasvatus.fi:443', () => {
      defineWindowLocation('staging.espoonvarhaiskasvatus.fi:443')
      expect(getEnvironment()).toEqual('staging')
    })
  })
})
