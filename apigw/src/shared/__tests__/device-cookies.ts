// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { createHash } from 'node:crypto'

import { beforeEach, describe, expect, it, jest } from '@jest/globals'
import { unsign } from 'cookie-signature'
import type express from 'express'

import {
  AUTH_HISTORY_COOKIE_PREFIX,
  filterValidDeviceAuthHistory,
  setDeviceAuthHistoryCookie
} from '../device-cookies.js'

describe('device-cookies', () => {
  const testUserId = 'user123'

  const createUserHash = (userId: string): string => {
    const hashGenerator = createHash('sha256')
    hashGenerator.update(userId)
    return hashGenerator.digest('hex')
  }

  describe('filterValidDeviceAuthHistory', () => {
    it('returns empty array when no device auth cookies are present', () => {
      const result = filterValidDeviceAuthHistory({})
      expect(result).toEqual([])
    })

    it('returns all valid hashes', () => {
      const hash1 = createUserHash('user1')
      const hash2 = createUserHash('user2')

      const cookies: Record<string, string> = {
        [`${AUTH_HISTORY_COOKIE_PREFIX}${hash1}`]: 's:' + hash1,
        [`${AUTH_HISTORY_COOKIE_PREFIX}${hash2}`]: 's:' + hash2,
        'other-cookie': 'value'
      }

      const result = filterValidDeviceAuthHistory(cookies)
      expect(result).toHaveLength(2)
      expect(result).toContain(hash1)
      expect(result).toContain(hash2)
    })

    it('calls callback when cookie-parser marks a cookie as invalid (false)', () => {
      const validHash = createUserHash('valid-user')
      const invalidHash = createUserHash('invalid-user')
      const nameValid = `${AUTH_HISTORY_COOKIE_PREFIX}${validHash}`
      const nameInvalid = `${AUTH_HISTORY_COOKIE_PREFIX}${invalidHash}`

      const signedCookies: Record<string, unknown> = {
        [nameValid]: validHash,
        [nameInvalid]: false
      }

      const cb = jest.fn()
      const result = filterValidDeviceAuthHistory(signedCookies, cb)
      expect(result).toEqual([validHash])
      expect(cb).toHaveBeenCalledWith(nameInvalid, invalidHash)
    })
  })

  describe('setDeviceAuthHistoryCookie', () => {
    const testSecret = 'test-cookie-secret'
    let mockResponse: jest.Mocked<express.Response>
    let cookieFn: jest.MockedFunction<
      (name: string, value: string, options: object) => void
    >

    beforeEach(() => {
      cookieFn =
        jest.fn<(name: string, value: string, options: object) => void>()
      mockResponse = {
        cookie: cookieFn
      } as unknown as jest.Mocked<express.Response>
    })

    it('sets cookie with a value that cookie-parser can unsign', () => {
      setDeviceAuthHistoryCookie(mockResponse, testUserId, testSecret)

      const expectedHash = createUserHash(testUserId)
      const expectedCookieName = `${AUTH_HISTORY_COOKIE_PREFIX}${expectedHash}`

      expect(cookieFn).toHaveBeenCalledTimes(1)
      const [name, value, options] = cookieFn.mock.calls[0]
      expect(name).toBe(expectedCookieName)

      // The value is manually signed with cookie-signature, prefixed with 's:'
      // Express will URL-encode this to 's%3A...' which cookie-parser handles
      expect(value).toMatch(/^s:/)
      expect(unsign(value.slice(2), testSecret)).toBe(expectedHash)

      expect(options).toEqual(
        expect.objectContaining({
          secure: true,
          httpOnly: true,
          sameSite: 'strict',
          signed: false,
          expires: expect.any(Date)
        })
      )
    })

    it('produces a value that fails unsign with a wrong secret', () => {
      setDeviceAuthHistoryCookie(mockResponse, testUserId, testSecret)

      const value = cookieFn.mock.calls[0][1]
      expect(unsign(value.slice(2), 'wrong-secret')).toBe(false)
    })

    it('creates different cookies for different users', () => {
      setDeviceAuthHistoryCookie(mockResponse, 'user1', testSecret)
      setDeviceAuthHistoryCookie(mockResponse, 'user2', testSecret)

      expect(cookieFn).toHaveBeenCalledTimes(2)

      const [name1, value1] = cookieFn.mock.calls[0]
      const [name2, value2] = cookieFn.mock.calls[1]

      expect(name1).not.toEqual(name2)
      expect(value1).not.toEqual(value2)
    })

    it('is deterministic for the same user and secret', () => {
      setDeviceAuthHistoryCookie(mockResponse, testUserId, testSecret)
      setDeviceAuthHistoryCookie(mockResponse, testUserId, testSecret)

      const [name1, value1] = cookieFn.mock.calls[0]
      const [name2, value2] = cookieFn.mock.calls[1]

      expect(name1).toEqual(name2)
      expect(value1).toEqual(value2)
    })
  })
})
