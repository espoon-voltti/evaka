// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import {
  Cancelled,
  Failure,
  isCancelled,
  Loading,
  Result,
  Success,
  withStaleCancellation
} from './api'

describe('utils/async', () => {
  describe('withCancellation', () => {
    it('ignores stale resolved values', async () => {
      const happyFunction = (value: string, delay: number) =>
        new Promise<Result<string>>((resolve) =>
          setTimeout(() => resolve(Success.of(value)), delay)
        )
      const wrapped = withStaleCancellation(happyFunction)

      let state: Result<string> = Loading.of()
      function setState(value: Result<string> | Cancelled) {
        if (!isCancelled(value)) {
          state = value
        }
      }

      await Promise.all([
        happyFunction('first slower wins', 100).then(setState),
        happyFunction('second faster loses', 5).then(setState)
      ])
      expect(state).toEqual(Success.of('first slower wins'))

      await Promise.all([
        wrapped('first slower loses', 100).then(setState),
        wrapped('second faster wins', 5).then(setState)
      ])
      expect(state).toEqual(Success.of('second faster wins'))

      await Promise.all([
        happyFunction('first faster loses', 5).then(setState),
        happyFunction('second slower wins', 100).then(setState)
      ])
      expect(state).toEqual(Success.of('second slower wins'))

      await Promise.all([
        wrapped('first faster loses', 5).then(setState),
        wrapped('second slower wins', 100).then(setState)
      ])
      expect(state).toEqual(Success.of('second slower wins'))
    })
  })
})

describe('api/Result', () => {
  it('Loading is a singleton', () => {
    expect(Loading.of()).toBe(Loading.of())
  })

  describe('map', () => {
    const id = <A>(a: A) => a
    const f = (a: string) => a + 'f'
    const g = (a: string) => a + 'g'

    it('identity - Loading', () => {
      const loading = Loading.of()
      expect(loading).toEqual(loading.map(id))
    })

    it('identity - Failure', () => {
      const failure = Failure.of({ message: 'error' })
      expect(failure).toEqual(failure.map(id))
    })

    it('identity - Success', () => {
      const success = Success.of('success')
      expect(success).toEqual(success.map(id))
    })

    it('composition - Loading', () => {
      const loading = Loading.of<string>()
      expect(loading.map((x) => f(g(x)))).toEqual(loading.map(g).map(f))
    })

    it('composition - Failure', () => {
      const failure = Failure.of<string>({ message: 'error' })
      expect(failure.map((x) => f(g(x)))).toEqual(failure.map(g).map(f))
    })

    it('composition - Success', () => {
      const success = Success.of('success')
      expect(success.map((x) => f(g(x)))).toEqual(success.map(g).map(f))
    })

    it('map does not change identity of instance of Loading', () => {
      const instance = Loading.of()
      expect(instance).toBe(instance.map(id))
    })

    it('map does not change identity of instance of Failure', () => {
      const instance = Failure.of({ message: 'error' })
      expect(instance).toBe(instance.map(id))
    })
  })
})
