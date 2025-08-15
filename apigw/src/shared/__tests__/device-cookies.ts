// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { createHash } from 'node:crypto'

import { beforeEach, describe, expect, it, jest } from '@jest/globals'
import type express from 'express'

import {
  AUTH_HISTORY_COOKIE_PREFIX,
  filterValidDeviceAuthHistory,
  setDeviceAuthHistoryCookie
} from '../device-cookies.js'

describe('device-cookies', () => {
  const testUserId = 'user123'

  // Helper function to create a user hash
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

    it('sets signed cookie with correct name and value', () => {
      setDeviceAuthHistoryCookie(mockResponse, testUserId)

      const expectedHash = createUserHash(testUserId)
      const expectedCookieName = `${AUTH_HISTORY_COOKIE_PREFIX}${expectedHash}`

      expect(cookieFn).toHaveBeenCalledTimes(1)
      expect(cookieFn).toHaveBeenCalledWith(
        expectedCookieName,
        expectedHash,
        expect.objectContaining({
          secure: true,
          httpOnly: true,
          sameSite: 'strict',
          signed: true
        })
      )
    })

    it('sets cookie with correct security attributes', () => {
      setDeviceAuthHistoryCookie(mockResponse, testUserId)

      expect(cookieFn).toHaveBeenCalledWith(
        expect.any(String),
        expect.any(String),
        expect.objectContaining({
          secure: true,
          httpOnly: true,
          sameSite: 'strict',
          signed: true,
          expires: expect.any(Date)
        })
      )
    })

    it('creates different cookies for different users', () => {
      const user1 = 'user1'
      const user2 = 'user2'

      setDeviceAuthHistoryCookie(mockResponse, user1)
      setDeviceAuthHistoryCookie(mockResponse, user2)

      expect(cookieFn).toHaveBeenCalledTimes(2)

      const call1 = cookieFn.mock.calls[0]
      const call2 = cookieFn.mock.calls[1]

      // Cookie names should be different
      expect(call1?.[0]).not.toEqual(call2?.[0])
      // Values should be different
      expect(call1?.[1]).not.toEqual(call2?.[1])
    })

    it('creates same cookie for same user with same secret', () => {
      setDeviceAuthHistoryCookie(mockResponse, testUserId)

      // Reset mock to test second call
      cookieFn.mockReset()

      setDeviceAuthHistoryCookie(mockResponse, testUserId)

      expect(cookieFn).toHaveBeenCalledTimes(1)

      // Both calls should produce identical results
      const expectedHash = createUserHash(testUserId)
      const expectedCookieName = `${AUTH_HISTORY_COOKIE_PREFIX}${expectedHash}`

      expect(cookieFn).toHaveBeenCalledWith(
        expectedCookieName,
        expectedHash,
        expect.any(Object)
      )
    })
  })
})
