// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import {
  Cancelled,
  isCancelled,
  Loading,
  Result,
  Success,
  withStaleCancellation
} from './index'

describe('utils/async', () => {
  describe('withCancellation', () => {
    it('ignores stale resolved values', async () => {
      const happyFunction = (value: string, delay: number) =>
        new Promise<Result<string>>((resolve) =>
          setTimeout(() => resolve(Success(value)), delay)
        )
      const wrapped = withStaleCancellation(happyFunction)

      let state: Result<string> = Loading()
      function setState(value: Result<string> | Cancelled) {
        if (!isCancelled(value)) {
          state = value
        }
      }

      await Promise.all([
        happyFunction('first slower wins', 100).then(setState),
        happyFunction('second faster loses', 5).then(setState)
      ])
      expect(state).toEqual(Success('first slower wins'))

      await Promise.all([
        wrapped('first slower loses', 100).then(setState),
        wrapped('second faster wins', 5).then(setState)
      ])
      expect(state).toEqual(Success('second faster wins'))

      await Promise.all([
        happyFunction('first faster loses', 5).then(setState),
        happyFunction('second slower wins', 100).then(setState)
      ])
      expect(state).toEqual(Success('second slower wins'))

      await Promise.all([
        wrapped('first faster loses', 5).then(setState),
        wrapped('second slower wins', 100).then(setState)
      ])
      expect(state).toEqual(Success('second slower wins'))
    })
  })
})
