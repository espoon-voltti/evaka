// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { describe, expect, it } from 'vitest'

import type { Result } from './api'
import { Failure, Loading, Success, combine } from './api'

describe('combine', () => {
  const success = Success.of(1)
  const reloading = success.reloading()
  const loading = Loading.of<number>()
  const failure = Failure.of({ message: 'foo' })

  it('is loading while at least one is loading and others are success', () => {
    const tests: [Result<unknown>, Result<unknown>, Result<unknown>][] = [
      [success, loading, success],
      [success, success, loading],
      [loading, success, success]
    ]
    tests.forEach((test) => {
      const { isLoading, isFailure, isSuccess } = combine(...test)
      expect(isLoading).toBe(true)
      expect(isFailure).toBe(false)
      expect(isSuccess).toBe(false)
    })
  })

  it('is failure when at least one is failure', () => {
    const tests: [Result<unknown>, Result<unknown>, Result<unknown>][] = [
      [success, loading, failure],
      [failure, success, loading],
      [loading, failure, success]
    ]
    tests.forEach((test) => {
      const { isLoading, isFailure, isSuccess } = combine(...test)
      expect(isLoading).toBe(false)
      expect(isFailure).toBe(true)
      expect(isSuccess).toBe(false)
    })
  })

  it('is reloading when at least one is reloading', () => {
    const tests: [Result<unknown>, Result<unknown>, Result<unknown>][] = [
      [success, success, reloading],
      [reloading, success, success],
      [reloading, success, reloading],
      [reloading, reloading, reloading]
    ]
    tests.forEach((test) => {
      const result = combine(...test)
      expect(result.isLoading).toBe(false)
      expect(result.isFailure).toBe(false)
      expect(result.isSuccess && result.isReloading).toBe(true)
    })
  })
})

describe('mapAll', () => {
  const success = Success.of('yippee')
  const loading = Loading.of<string>()
  const failure = Failure.of<string>({ message: 'foo' })
  const reloading = success.reloading()
  const mapper = {
    loading() {
      return 'loading'
    },
    failure() {
      return 'failure'
    },
    success(v: string, isReloading: boolean) {
      return v + (isReloading ? ' (reloading)' : '')
    }
  }

  it('works', () => {
    expect(loading.mapAll(mapper)).toEqual('loading')
    expect(failure.mapAll(mapper)).toEqual('failure')
    expect(success.mapAll(mapper)).toEqual('yippee')
    expect(reloading.mapAll(mapper)).toEqual('yippee (reloading)')
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
