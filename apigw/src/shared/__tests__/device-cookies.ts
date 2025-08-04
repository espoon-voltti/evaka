// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { createHash, createHmac } from 'node:crypto'

import { beforeEach, describe, expect, it, jest } from '@jest/globals'
import express from 'express'

import {
  AUTH_HISTORY_COOKIE_PREFIX,
  cookieSignatureOk,
  filterValidDeviceAuthHistory,
  setDeviceAuthHistoryCookie
} from '../device-cookies.js'

describe('device-cookies', () => {
  const testSecret = 'test-secret-key-for-hmac-signing'
  const testUserId = 'user123'

  // Helper function to create a valid signature for a hash
  const createValidSignature = (hash: string, secret: string): string => {
    const hmac = createHmac('sha256', secret)
    hmac.update(hash)
    return hmac.digest('hex')
  }

  // Helper function to create a user hash
  const createUserHash = (userId: string): string => {
    const hashGenerator = createHash('sha256')
    hashGenerator.update(userId)
    return hashGenerator.digest('hex')
  }

  describe('cookieSignatureOk', () => {
    it('returns true for valid signature', () => {
      const hash = 'test-hash-value'
      const validSignature = createValidSignature(hash, testSecret)

      expect(cookieSignatureOk(hash, validSignature, testSecret)).toBe(true)
    })

    it('returns false for invalid signature', () => {
      const hash = 'test-hash-value'
      const invalidSignature = 'invalid-signature-value'

      expect(cookieSignatureOk(hash, invalidSignature, testSecret)).toBe(false)
    })

    it('returns false for signature created with different secret', () => {
      const hash = 'test-hash-value'
      const signatureWithDifferentSecret = createValidSignature(
        hash,
        'different-secret'
      )

      expect(
        cookieSignatureOk(hash, signatureWithDifferentSecret, testSecret)
      ).toBe(false)
    })
  })

  describe('filterValidDeviceAuthHistory', () => {
    it('returns empty array when no device auth cookies are present', () => {
      const cookies = {
        'other-cookie': 'value',
        'session-cookie': 'session-value'
      }

      const result = filterValidDeviceAuthHistory(cookies, testSecret)
      expect(result).toEqual([])
    })

    it('returns valid hashes for cookies with correct signatures', () => {
      const hash1 = createUserHash('user1')
      const hash2 = createUserHash('user2')
      const validSignature1 = createValidSignature(hash1, testSecret)
      const validSignature2 = createValidSignature(hash2, testSecret)

      const cookies = {
        [`${AUTH_HISTORY_COOKIE_PREFIX}${hash1}`]: validSignature1,
        [`${AUTH_HISTORY_COOKIE_PREFIX}${hash2}`]: validSignature2,
        'other-cookie': 'value'
      }

      const result = filterValidDeviceAuthHistory(cookies, testSecret)
      expect(result).toHaveLength(2)
      expect(result).toContain(hash1)
      expect(result).toContain(hash2)
    })

    it('filters out cookies with invalid signatures', () => {
      const validHash = createUserHash('valid-user')
      const invalidHash = createUserHash('invalid-user')
      const validSignature = createValidSignature(validHash, testSecret)
      const invalidSignature = 'invalid-signature'

      const cookies = {
        [`${AUTH_HISTORY_COOKIE_PREFIX}${validHash}`]: validSignature,
        [`${AUTH_HISTORY_COOKIE_PREFIX}${invalidHash}`]: invalidSignature
      }

      const result = filterValidDeviceAuthHistory(cookies, testSecret)
      expect(result).toHaveLength(1)
      expect(result).toContain(validHash)
      expect(result).not.toContain(invalidHash)
    })

    it('calls invalidSignatureCallback for invalid signatures', () => {
      const validHash = createUserHash('valid-user')
      const invalidHash = createUserHash('invalid-user')
      const validSignature = createValidSignature(validHash, testSecret)
      const invalidSignature = 'invalid-signature'

      const cookies = {
        [`${AUTH_HISTORY_COOKIE_PREFIX}${validHash}`]: validSignature,
        [`${AUTH_HISTORY_COOKIE_PREFIX}${invalidHash}`]: invalidSignature
      }

      const invalidSignatureCallback = jest.fn()
      const result = filterValidDeviceAuthHistory(
        cookies,
        testSecret,
        invalidSignatureCallback
      )

      expect(result).toHaveLength(1)
      expect(invalidSignatureCallback).toHaveBeenCalledTimes(1)
      expect(invalidSignatureCallback).toHaveBeenCalledWith(
        `${AUTH_HISTORY_COOKIE_PREFIX}${invalidHash}`,
        invalidHash
      )
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

    it('sets cookie with correct name and signature', () => {
      setDeviceAuthHistoryCookie(mockResponse, testUserId, testSecret)

      const expectedHash = createUserHash(testUserId)
      const expectedSignature = createValidSignature(expectedHash, testSecret)
      const expectedCookieName = `${AUTH_HISTORY_COOKIE_PREFIX}${expectedHash}`

      expect(cookieFn).toHaveBeenCalledTimes(1)
      expect(cookieFn).toHaveBeenCalledWith(
        expectedCookieName,
        expectedSignature,
        expect.objectContaining({
          secure: true,
          httpOnly: true,
          sameSite: 'strict'
        })
      )
    })

    it('sets cookie with correct security attributes', () => {
      setDeviceAuthHistoryCookie(mockResponse, testUserId, testSecret)

      expect(cookieFn).toHaveBeenCalledWith(
        expect.any(String),
        expect.any(String),
        expect.objectContaining({
          secure: true,
          httpOnly: true,
          sameSite: 'strict',
          expires: expect.any(Date)
        })
      )
    })

    it('creates different cookies for different users', () => {
      const user1 = 'user1'
      const user2 = 'user2'

      setDeviceAuthHistoryCookie(mockResponse, user1, testSecret)
      setDeviceAuthHistoryCookie(mockResponse, user2, testSecret)

      expect(cookieFn).toHaveBeenCalledTimes(2)

      const call1 = cookieFn.mock.calls[0]
      const call2 = cookieFn.mock.calls[1]

      // Cookie names should be different
      expect(call1?.[0]).not.toEqual(call2?.[0])
      // Signatures should be different
      expect(call1?.[1]).not.toEqual(call2?.[1])
    })

    it('creates same cookie for same user with same secret', () => {
      setDeviceAuthHistoryCookie(mockResponse, testUserId, testSecret)

      // Reset mock to test second call
      cookieFn.mockReset()

      setDeviceAuthHistoryCookie(mockResponse, testUserId, testSecret)

      expect(cookieFn).toHaveBeenCalledTimes(1)

      // Both calls should produce identical results
      const expectedHash = createUserHash(testUserId)
      const expectedSignature = createValidSignature(expectedHash, testSecret)
      const expectedCookieName = `${AUTH_HISTORY_COOKIE_PREFIX}${expectedHash}`

      expect(cookieFn).toHaveBeenCalledWith(
        expectedCookieName,
        expectedSignature,
        expect.any(Object)
      )
    })

    it('creates different signatures with different secrets', () => {
      const secret1 = 'secret1'
      const secret2 = 'secret2'

      setDeviceAuthHistoryCookie(mockResponse, testUserId, secret1)
      setDeviceAuthHistoryCookie(mockResponse, testUserId, secret2)

      expect(cookieFn).toHaveBeenCalledTimes(2)

      const call1 = cookieFn.mock.calls[0]
      const call2 = cookieFn.mock.calls[1]

      // Cookie names should be the same (same user)
      expect(call1?.[0]).toEqual(call2?.[0])
      // But signatures should be different (different secrets)
      expect(call1?.[1]).not.toEqual(call2?.[1])
    })
  })
})
